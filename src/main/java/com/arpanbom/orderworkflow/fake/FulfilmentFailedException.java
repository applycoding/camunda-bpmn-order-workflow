package com.arpanbom.orderworkflow.fake;

public class FulfilmentFailedException extends RuntimeException {

    public FulfilmentFailedException(String message) {
        super(message);
    }
}
