package online.flowerinsnow.dns_j.object.record;

import online.flowerinsnow.dns_j.exception.UnexpectedException;

public record SRVRecord(int priority, int weight, int port, String target) implements Cloneable {
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new UnexpectedException(e);
        }
    }
}
