
package com.careeats.backend.ord.domain;

public interface OrderObserver {
    void update(StateTransitionRecord record);
}
