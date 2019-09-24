package com.template;

import com.google.common.collect.ImmutableList;
import com.template.contracts.RequestToBankContract;
import com.template.states.LoanRequestState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static com.template.contracts.RequestToBankContract.REQUEST_TO_BANK_CONTRACT_ID;
import static net.corda.testing.node.NodeTestUtils.ledger;
import static net.corda.testing.node.NodeTestUtils.transaction;

public class ContractTests {
    static private final MockServices ledgerServices = new MockServices();
    static private TestIdentity dealer = new TestIdentity(new CordaX500Name("dealer", "Hyderabad", "IN"));
    static private TestIdentity bank = new TestIdentity(new CordaX500Name("bank", "Bangalore", "IN"));

    private static int amount = 15000;
    private static String companyName = "Varun Hyundai";
    private static boolean amountTransfer = false;

    private static LoanRequestState dealerBankState = new LoanRequestState(dealer.getParty(), bank.getParty(), companyName,amount,amountTransfer,new UniqueIdentifier());

    @Test
    public void transactionMustIncludeCreateCommand() {

        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(REQUEST_TO_BANK_CONTRACT_ID, dealerBankState);
                tx.fails();
                tx.command(ImmutableList.of(dealer.getParty().getOwningKey(), bank.getParty().getOwningKey()), new RequestToBankContract.Commands.RequestForLoan());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    /***This test case is for when PartyA contacts Party B (when dealer send loan application to bank) **/
    @Test
    public void transactionMustHaveNoInputs() {

        transaction(ledgerServices,tx -> {
            tx.output(REQUEST_TO_BANK_CONTRACT_ID, dealerBankState);
            tx.command(ImmutableList.of(dealer.getParty().getOwningKey(), bank.getParty().getOwningKey()), new RequestToBankContract.Commands.RequestForLoan());
            tx.verifies();
            return null;
        });

        /**** uncomment for failure criteria **/
           /* transaction(ledgerServices,tx -> {
                tx.input(REQUEST_TO_BANK_CONTRACT_ID, dealerBankState);
                tx.output(REQUEST_TO_BANK_CONTRACT_ID, dealerBankState);
                tx.command(ImmutableList.of(dealer.getParty().getOwningKey(), bank.getParty().getOwningKey()), new RequestToBankContract.Commands.RequestForLoan());
                tx.failsWith("No inputs should be consumed when issuing .");
                return null;
            });*/
    }

    /***This test case is for when PartyA contacts Party B (when dealer send loan application to bank) **/
    @Test
    public void transactionMustHaveOneOutput() {

        transaction(ledgerServices,tx -> {
            tx.output(REQUEST_TO_BANK_CONTRACT_ID, dealerBankState);
            tx.command(ImmutableList.of(dealer.getPublicKey(), bank.getPublicKey()), new RequestToBankContract.Commands.RequestForLoan());
            tx.verifies();
            return null;
        });

        /**** uncomment for failure criteria **/
        /* transaction(ledgerServices,tx -> {
            tx.output(dealer_CONTRACT_ID, dealerBankState);
            tx.output(dealer_CONTRACT_ID, dealerBankState);
                tx.command(ImmutableList.of(dealer.getPublicKey(), bank.getPublicKey()), new RequestToBankContract.Commands.RequestForLoan());
                tx.failsWith("Only one output state should be created.");
                return null;
            });*/
    }

    /***This test case is for when PartyA contacts Party B (when dealer send loan application to bank) **/
    @Test
    public void lenderMustSignTransaction() {

        transaction(ledgerServices,tx -> {
            tx.output(REQUEST_TO_BANK_CONTRACT_ID, new LoanRequestState(dealer.getParty(), bank.getParty(), companyName,amount,amountTransfer,new UniqueIdentifier()));
            tx.command(ImmutableList.of(dealer.getPublicKey(),bank.getPublicKey()), new RequestToBankContract.Commands.RequestForLoan());
            tx.verifies();
            return null;
        });

        /****uncomment for failure criteria **/
       /* transaction(ledgerServices,tx -> {
                tx.output(dealer_CONTRACT_ID, new LoanRequestState(dealer.getParty(), bank.getParty(), companyName,amount,amountTransfer,new UniqueIdentifier()));
                tx.command(ImmutableList.of(bank.getPublicKey()), new RequestToBankContract.Commands.RequestForLoan());
                tx.failsWith("All of the participants must be signers.");
                return null;
            });*/
    }

    /***This test case is for when PartyA contacts Party B (when dealer agency send loan application to bank) **/
    @Test
    public void borrowerMustSignTransaction() {

        transaction(ledgerServices,tx -> {
            tx.output(REQUEST_TO_BANK_CONTRACT_ID, new LoanRequestState(dealer.getParty(), bank.getParty(), companyName,amount,amountTransfer,new UniqueIdentifier()));
            tx.command(ImmutableList.of(dealer.getPublicKey(),bank.getPublicKey()), new RequestToBankContract.Commands.RequestForLoan());
            tx.verifies();
            return null;
        });
        /**** uncomment for failure criteria ****/
         /*transaction(ledgerServices,tx -> {
                tx.output(dealer_CONTRACT_ID, new LoanRequestState(dealer.getParty(), bank.getParty(), companyName,amount,amountTransfer,new UniqueIdentifier()));
                tx.command(ImmutableList.of(dealer.getPublicKey(),bank.getPublicKey()), new RequestToBankContract.Commands.RequestForLoan());
                tx.failsWith("All of the participants must be signers.");
                return null;
        });*/
    }

    /***This test case is for when PartyA contacts Party B (when dealer agency send loan application to bank) **/
    @Test
    public void lenderIsNotBorrower() {

        transaction(ledgerServices,tx -> {
            tx.output(REQUEST_TO_BANK_CONTRACT_ID, new LoanRequestState(dealer.getParty(), bank.getParty(), companyName,amount,amountTransfer,new UniqueIdentifier()));
            tx.command(ImmutableList.of(dealer.getPublicKey(), bank.getPublicKey()),  new RequestToBankContract.Commands.RequestForLoan());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void cannotCreateNegativeValue() {

        transaction(ledgerServices,tx -> {
            tx.output(REQUEST_TO_BANK_CONTRACT_ID,new LoanRequestState(dealer.getParty(), bank.getParty(), companyName,amount,amountTransfer,new UniqueIdentifier()));
            tx.command(ImmutableList.of(dealer.getPublicKey(), bank.getPublicKey()), new RequestToBankContract.Commands.RequestForLoan());
            tx.verifies();
            return null;
        });
    }



    /***This test case is for when PartyB contacts Party A (when bank send loan application status to dealer) **/
    @Test
    public void transactionMustHaveOneInputs() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices,tx -> {
            tx.input(REQUEST_TO_BANK_CONTRACT_ID,dealerBankState);
            tx.output(REQUEST_TO_BANK_CONTRACT_ID, dealerBankState);
            tx.command(ImmutableList.of(dealer.getParty().getOwningKey(), bank.getParty().getOwningKey()), new RequestToBankContract.Commands.ResponseFromBank());
            tx.verifies();
            return null;
        });
    }

    /***This test case is for when PartyB contacts Party A (when bank send loan application Status to dealer) **/
    @Test
    public void dealerBankStateMustHaveFinalOneOutput() {
        /**** This test case also checks the both parties actually sign the transaction ***/
        transaction(ledgerServices,tx -> {
            tx.input(REQUEST_TO_BANK_CONTRACT_ID,dealerBankState);
            tx.output(REQUEST_TO_BANK_CONTRACT_ID, dealerBankState);
            tx.command(ImmutableList.of(dealer.getPublicKey(), bank.getPublicKey()), new RequestToBankContract.Commands.ResponseFromBank());
            tx.verifies();
            return null;
        });
    }}