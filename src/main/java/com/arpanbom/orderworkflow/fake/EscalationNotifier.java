package com.arpanbom.orderworkflow.fake;

public interface EscalationNotifier {

    void notifyApprovers(EscalationNotice notice);
}
