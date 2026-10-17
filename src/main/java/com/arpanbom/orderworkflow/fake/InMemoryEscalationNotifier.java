package com.arpanbom.orderworkflow.fake;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

@Component
public class InMemoryEscalationNotifier implements EscalationNotifier {

    private final CopyOnWriteArrayList<EscalationNotice> notices = new CopyOnWriteArrayList<>();

    @Override
    public void notifyApprovers(EscalationNotice notice) {
        notices.add(notice);
    }

    public List<EscalationNotice> notices() {
        return List.copyOf(notices);
    }
}
