package com.pgmanagement.enums;

public enum ElectricitySplitMethod {
    TIME_WEIGHTED,     // Split based on days each tenant stayed
    EQUAL              // Equal split regardless of stay duration
}
