package com.template.pojo;

public class LoanResponse {

    private String party;

    private String linearId;

    public String getParty() {
        return party;
    }

    public void setParty(String party) {
        this.party = party;
    }

    public String getLinearId() {
        return linearId;
    }

    public void setLinearId(String linearId) {
        this.linearId = linearId;
    }

    @Override
    public String toString() {
        return "LoanResponse{" +
                "party='" + party + '\'' +
                ", linearId='" + linearId + '\'' +
                '}';
    }
}
