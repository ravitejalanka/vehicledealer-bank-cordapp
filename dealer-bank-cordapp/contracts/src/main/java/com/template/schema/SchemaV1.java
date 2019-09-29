package com.template.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * @author RaviTeja.l
 */
public class SchemaV1 extends MappedSchema {
    public SchemaV1(){
        super(Schema.class, 1, ImmutableList.of(PersistLoanStatus.class));
    }

        @Entity
        @Table(name = "loan_status")
        public static class PersistLoanStatus extends PersistentState {
            @Column(name = "dealer") private final String dealer;
            @Column(name = "bank") private final String bank;
            @Column(name = "company_name") private final String dealerCompanyName;
            @Column(name = "amount") private final int amount;
            @Column(name = "amount_transfer") private final boolean amountTransfer;
            @Column(name = "linear_id") private final UUID linearIdLoanReq;


            public PersistLoanStatus(String dealer, String bank, String dealerCompanyName, int amount,boolean amountTransfer, UUID linearIdLoanReq) {
                this.dealer = dealer;
                this.bank = bank;
                this.dealerCompanyName = dealerCompanyName;
                this.amount = amount;
                this.amountTransfer = amountTransfer;
                this.linearIdLoanReq = linearIdLoanReq;
            }

            public PersistLoanStatus() {
                this.dealerCompanyName = null;
                this.amountTransfer = false;
                this.dealer = null;
                this.bank = null;
                this.amount = 0;
                this.linearIdLoanReq = null;
            }

            public String getDealer() {
                return dealer;
            }

            public String getBank() {
                return bank;
            }

            public int getAmount() {
                return amount;
            }

            public boolean isAmountTransfer() {
                return amountTransfer;
            }
            
            public String getDealerCompanyName(){ return dealerCompanyName;}

            public UUID getLinearIdLoanReq() { return linearIdLoanReq; }

        }
}
