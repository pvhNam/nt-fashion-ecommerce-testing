package model;

public class LoyaltyReward {
    private final boolean newlyIssued;
    private final String voucherCode;
    private final Membership membership;

    public LoyaltyReward(boolean newlyIssued, String voucherCode, Membership membership) {
        this.newlyIssued = newlyIssued;
        this.voucherCode = voucherCode;
        this.membership = membership;
    }

    public boolean isNewlyIssued() {
        return newlyIssued;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public Membership getMembership() {
        return membership;
    }
}
