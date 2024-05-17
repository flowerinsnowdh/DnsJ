package online.flowerinsnow.dns_j.object.record;

import online.flowerinsnow.dns_j.exception.UnexpectedException;

public record MXRecord(int preference, String mailExchange) implements Cloneable {
    @Override
    public MXRecord clone() {
        try {
            return (MXRecord) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new UnexpectedException(e);
        }
    }
}
