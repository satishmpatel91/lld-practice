package com.parkinglot;

import com.parkinglot.constants.GateType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Gate {
    private final String gateId;
    private final GateType gateType;
}
