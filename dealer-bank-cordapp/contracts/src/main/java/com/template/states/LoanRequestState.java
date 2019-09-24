package com.template.states;

import com.google.common.collect.ImmutableList;
import com.template.contracts.RequestToBankContract;
import com.template.schema.SchemaV1;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author RaviTeja.l
 */
@BelongsToContract(RequestToBankContract.class)
public class LoanRequestState  implements LinearState, QueryableState {

    private Party dealer;
    private Party bank;
    private int amount;
    private String dealerCompanyName;
    private boolean amountTransfer;
    private final UniqueIdentifier linearIdLoanReq;

    @ConstructorForDeserialization
    public LoanRequestState(Party dealer, Party bank, String dealerCompanyName,int amount, boolean amountTransfer, UniqueIdentifier linearIdLoanReq) {
        this.dealer = dealer;
        this.bank = bank;
        this.dealerCompanyName = dealerCompanyName;
        this.amount = amount;
        this.amountTransfer = amountTransfer;
        this.linearIdLoanReq = linearIdLoanReq;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearIdLoanReq;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(dealer,bank);
    }


//*****************************Getters and Setters******************************//
    public Party getDealer() {
        return dealer;
    }

    public Party getBank() {
        return bank;
    }

    public int getAmount() {
        return amount;
    }

    public String getDealerCompanyName(){
        return dealerCompanyName;
    }

    @NotNull
    @Override
    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {
        if (schema instanceof SchemaV1) {
            return new SchemaV1.PersistLoanStatus(
                    this.dealer.getName().toString(),
                    this.bank.getName().toString(),
                    this.dealerCompanyName,
                    this.amount,
                    this.amountTransfer,
                    this.linearIdLoanReq.getId());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new SchemaV1());
    }
}
