package com.martingoe.cubelang.backend.instructionselection

import com.martingoe.cubelang.common.ASMEmitter
import com.martingoe.cubelang.common.NormalType
import com.martingoe.cubelang.common.NormalTypes
import com.martingoe.cubelang.common.RegisterConfig
import com.martingoe.cubelang.common.errors.ErrorManager
import com.martingoe.cubelang.common.ir.*
import java.util.LinkedList
import java.util.PriorityQueue

/**
 * A currently active lifetime interval of a specific register.
 *
 * @param regIndex The index of the currently active register
 */
data class CurrentActiveRegisterLiveInterval(
    val regIndex: Int,
    val start: Int,
    val end: Int
)

/**
 * An interval of the lifetime of a virtual register.
 */
data class VirtualRegisterLiveInterval(
    val virtualRegIndex: Int,
    val start: Int,
    var end: Int
)

/**
 * Assignment of real registers to the virtual registers given in the intermediate representation.
 *
 * @param emitter The emitter whose IR values are to be assigned registers
 */
class RegisterAllocation(private val emitter: ASMEmitter, private val errorManager: ErrorManager) {
    var registerSaveIndices: MutableMap<Int, List<Int>> = HashMap()
    /**
     * Runs the linear scan register allocation algorithm on the current results of the [[emitter]].
     */
    fun linearScanRegisterAllocation() {
        registerSaveIndices.clear()
        val intervals = getLiveIntervals(emitter.resultIRValues)
        val freeRegisters = PriorityQueue<Int>()
        freeRegisters.addAll(0 until RegisterConfig.REGISTER_TEMP_COUNT)
        val active = LinkedList<CurrentActiveRegisterLiveInterval>()
        for (i in intervals) {
            expireOldIntervals(i, active, freeRegisters)
            if (active.size == RegisterConfig.REGISTER_TEMP_COUNT - 1)
                spillAtInterval(i)
            else {
                val regIndex = freeRegisters.poll()
                setAllocatedRegister(regIndex, i, active)
                active.addSortedIncreasingEndPoint(CurrentActiveRegisterLiveInterval(regIndex, i.start, i.end))
            }
        }

        saveRegisters()
    }

    private fun saveRegisters() {
        registerSaveIndices.toSortedMap(Comparator.reverseOrder()).forEach { (key, value) ->
            for ((i, reg) in value.withIndex()) {
                emitter.resultIRValues.add(key + i, IRValue(IRType.PUSH_REG, TemporaryRegister(reg, reg), null, NormalType(NormalTypes.I64)))
                emitter.resultIRValues.add(key + i + 2, IRValue(IRType.POP_REG, TemporaryRegister(reg, reg), null, NormalType(NormalTypes.I64)))
            }
        }
    }

    private fun setAllocatedRegister(regIndex: Int, interval: VirtualRegisterLiveInterval, currentActiveRegisterLiveIntervals: List<CurrentActiveRegisterLiveInterval>) {
        for (i in interval.end downTo interval.start) {
            emitter.resultIRValues[i].arg0?.let { setAllocatedRegister(it, regIndex, interval.virtualRegIndex) }
            emitter.resultIRValues[i].arg1?.let { setAllocatedRegister(it, regIndex, interval.virtualRegIndex) }

            if(emitter.resultIRValues[i].type == IRType.CALL){
                registerSaveIndices[i] = currentActiveRegisterLiveIntervals.map { it.regIndex }.toList()
            }
        }
    }

    private fun setAllocatedRegister(irValue: ValueType, regIndex: Int, virtualRegIndex: Int) {
        when (irValue) {
            is TemporaryRegister -> if (irValue.index == virtualRegIndex) irValue.allocatedIndex = regIndex
            is RegOffset -> if (irValue.temporaryRegister.index == virtualRegIndex) irValue.temporaryRegister.allocatedIndex = regIndex
            is FramePointerOffset -> if (irValue.temporaryRegister != null && irValue.temporaryRegister.index == virtualRegIndex) irValue.temporaryRegister.allocatedIndex =
                regIndex
        }
    }

    private fun spillAtInterval(i: VirtualRegisterLiveInterval) {
        errorManager.error(
            -1,
            -1,
            "Unfortunately, the currently available registers do not suffice. Please simplify any expressions requiring many registers."
        )
    }

    private fun expireOldIntervals(
        i: VirtualRegisterLiveInterval,
        active: LinkedList<CurrentActiveRegisterLiveInterval>,
        freeRegisters: PriorityQueue<Int>
    ) {
        // Old end indices < current start index means that interval is unused and the allocated register can be used again
        active.filter { it.end < i.start }.forEach { freeRegisters.add(it.regIndex) }
        active.removeIf { it.end < i.start }
    }

    private fun getLiveIntervals(resultIRValues: List<IRValue>): LinkedList<VirtualRegisterLiveInterval> {
        val resultList = LinkedList<VirtualRegisterLiveInterval>()
        for (i in resultIRValues.indices) {
            addLiveInterval(resultIRValues[i].arg0, resultList, i)
            addLiveInterval(resultIRValues[i].arg1, resultList, i)
        }
        return resultList
    }

    private fun addLiveInterval(value: ValueType?, resultList: LinkedList<VirtualRegisterLiveInterval>, index: Int) {
        value?.let {
            if (value is TemporaryRegister) {
                // Register not yet accounted for
                addSingularIndex(resultList, value, index)
            }
            if (value is RegOffset) {
                // Register not yet accounted for
                addSingularIndex(resultList, value.temporaryRegister, index)
            }
            if (value is FramePointerOffset && value.temporaryRegister != null) {
                addSingularIndex(resultList, value.temporaryRegister, index)
            }
        }
    }

    private fun addSingularIndex(
        resultList: LinkedList<VirtualRegisterLiveInterval>,
        value: TemporaryRegister,
        index: Int
    ) {
        if (resultList.none { it.virtualRegIndex == value.index }) {
            resultList.insertSortedStartpoint(VirtualRegisterLiveInterval(value.index, index, index))
        } else {
            val indexOfFirst = resultList.indexOfFirst { it.virtualRegIndex == value.index }
            resultList[indexOfFirst].end = index
        }
    }
}

private fun LinkedList<CurrentActiveRegisterLiveInterval>.addSortedIncreasingEndPoint(currentActiveRegisterLiveInterval: CurrentActiveRegisterLiveInterval) {
    this.add(this.indexOfLast { it.end <= currentActiveRegisterLiveInterval.end } + 1, currentActiveRegisterLiveInterval)

}

private fun LinkedList<VirtualRegisterLiveInterval>.insertSortedStartpoint(virtualRegisterLiveInterval: VirtualRegisterLiveInterval) {
    this.add(this.indexOfLast { it.start <= virtualRegisterLiveInterval.start } + 1, virtualRegisterLiveInterval)
}
