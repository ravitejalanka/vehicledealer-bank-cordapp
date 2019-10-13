package com.template.pojo;

public class LoanRequest {

    private String party;
    private String amount;
    private String dealerName;

    @Override
    public String toString() {
        return "LoanRequest{" +
                "party='" + party + '\'' +
                ", amount=" + amount +
                ", dealerName='" + dealerName + '\'' +
                '}';
    }

    public String getParty() {
        return party;
    }

    public void setParty(String party) {
        this.party = party;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getDealerName() {
        return dealerName;
    }

    public void setDealerName(String dealerName) {
        this.dealerName = dealerName;
    }
}
