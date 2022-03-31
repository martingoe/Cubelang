package com.cubearrow.cubelang.common.definitions

import com.cubearrow.cubelang.common.Type

/**
 * The class used to save struct definitions
 * @param name the name of the struct
 * @param variables A list of variables with both the name and type
 */
class Struct(val name: String, val variables: List<Pair<String, Type>>)