package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.template.contracts.RequestToBankContract;
import com.template.states.LoanRequestState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * @author RaviTeja.l
 */
public class RequestBankFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class RequestToBank extends FlowLogic<SignedTransaction>{

        private final Party bank;
        private int amount;
        private String company;

        public RequestToBank(Party bank, int amount,String company) {
            this.bank = bank;
            this.amount = amount;
            this.company = company;
        }

        private final ProgressTracker.Step REQUEST_LOAN = new ProgressTracker.Step("Dealer sending Loan application for bank");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        private final ProgressTracker.Step BANK_RESPONSE = new ProgressTracker.Step("Sending response to dealer agency from Bank");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };

        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };


        private final ProgressTracker progressTracker = new ProgressTracker(
                REQUEST_LOAN,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                BANK_RESPONSE,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //Get the notary
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            /* STEP-1 Starts*/
            progressTracker.setCurrentStep(REQUEST_LOAN);

            Party dealer = getOurIdentity();

            // Create and Request for Loan Proposal
            LoanRequestState  loanRequestState = new LoanRequestState(dealer, bank,company,amount, false,new UniqueIdentifier());
            final Command<RequestToBankContract.Commands.RequestForLoan> requestCommand= new Command<>(new RequestToBankContract.Commands.RequestForLoan(), ImmutableList.of(loanRequestState.getBank().getOwningKey(), loanRequestState.getDealer().getOwningKey()));

            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(loanRequestState, RequestToBankContract.REQUEST_TO_BANK_CONTRACT_ID)
                    .addCommand(requestCommand);
            /* STEP-1 Ends*/

            /* STEP-2 Starts*/
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            txBuilder.verify(getServiceHub());
            /* STEP-2  Ends*/

            /* STEP-3 Starts*/
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
            /* STEP-3 Ends*/

            /* STEP-4 Starts*/
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(bank);
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx,
                                                                                        ImmutableSet.of(otherPartySession),
                                                                                        CollectSignaturesFlow.Companion.tracker()));
            /* STEP-4 Ends*/


            /* STEP-5 Starts*/
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            //Notarise and record the transaction in both party vaults.
            return subFlow(new FinalityFlow(fullySignedTx,
                                            ImmutableSet.of(otherPartySession),
                                            FINALISING_TRANSACTION.childProgressTracker()));
            /* STEP-5 Ends*/
        }
    }

    @InitiatedBy(RequestToBank.class)
    public static class BankVerification extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartySession;

        public BankVerification(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                public SignTxFlow(FlowSession otherSideSession, ProgressTracker progressTracker) {
                    super(otherSideSession, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be a transaction between bank and Dealer.", output instanceof LoanRequestState);
                        return null;
                    });
                }
            }
            final SignTxFlow signTxFlow = new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker());
            final SecureHash txId = subFlow(signTxFlow).getId();

            return subFlow(new ReceiveFinalityFlow(otherPartySession, txId));
        }
    }

}
