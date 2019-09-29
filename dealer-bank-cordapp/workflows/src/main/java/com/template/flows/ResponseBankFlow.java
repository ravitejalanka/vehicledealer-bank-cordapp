package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.template.contracts.RequestToBankContract;
import com.template.states.LoanRequestState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class ResponseBankFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final Party dealer;
        UniqueIdentifier linearIdLoanReqDataState = null;


        public Initiator(Party dealer, UniqueIdentifier linearIdLoanReqDataState) {
            this.dealer = dealer;
            this.linearIdLoanReqDataState = linearIdLoanReqDataState;
        }
        private final ProgressTracker.Step BANK_RESPONSE = new ProgressTracker.Step("Sending response to Dealer Node from Bank");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
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
                BANK_RESPONSE,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        public UniqueIdentifier getLinearIdLoanReqDataState() {
            return linearIdLoanReqDataState;
        }
        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            final Party bank = getOurIdentity();
            StateAndRef<LoanRequestState> inputState = null;

            //STEP-1 Starts
            progressTracker.setCurrentStep(BANK_RESPONSE);


            QueryCriteria criteriaForBankVault = new QueryCriteria.LinearStateQueryCriteria(
                    null,
                    ImmutableList.of(linearIdLoanReqDataState),
                    Vault.StateStatus.UNCONSUMED,
                    null);
            List<StateAndRef<LoanRequestState>> loanRequestStateList = getServiceHub().getVaultService().queryBy(LoanRequestState.class, criteriaForBankVault).getStates();
            if ((loanRequestStateList == null) || (loanRequestStateList.isEmpty())) {
                throw new FlowException("Linearid with id %s not found." + linearIdLoanReqDataState );
            } else {
                inputState = loanRequestStateList.get(0);
            }
            LoanRequestState requestState=inputState.getState().getData();
            LoanRequestState loanRequestStateObj = new LoanRequestState(dealer, bank, requestState.getDealerCompanyName(),requestState.getAmount(), true,linearIdLoanReqDataState);
            final Command<RequestToBankContract.Commands.ResponseFromBank> loanNotificationCommand = new Command<RequestToBankContract.Commands.ResponseFromBank>(new RequestToBankContract.Commands.ResponseFromBank(), ImmutableList.of(getOurIdentity().getOwningKey(), dealer.getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputState)
                    .addOutputState(loanRequestStateObj, RequestToBankContract.REQUEST_TO_BANK_CONTRACT_ID)
                    .addCommand(loanNotificationCommand);

            //STEP-2 Starts
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            txBuilder.verify(getServiceHub());
            //STEP-3 Starts
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
            //STEP-4
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(dealer);
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(otherPartySession), CollectSignaturesFlow.Companion.tracker()));
            //STEP-5
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            //Notarise and record the transaction in both party vaults.
            return subFlow(new FinalityFlow(fullySignedTx,ImmutableSet.of(otherPartySession),FINALISING_TRANSACTION.childProgressTracker()));

        }
    }



    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {
        private final FlowSession otherPartySession;
        public Acceptor(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            class SignTxFlow extends  SignTransactionFlow {
                public SignTxFlow(FlowSession otherSideSession, ProgressTracker progressTracker) {
                    super(otherSideSession, progressTracker);

                }

                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("Error ...!! This must be Bank's transaction to Dealer (LoanRequestState transaction).", output instanceof LoanRequestState);
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
