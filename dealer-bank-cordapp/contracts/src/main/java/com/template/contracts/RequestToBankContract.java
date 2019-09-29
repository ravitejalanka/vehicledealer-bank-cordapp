package com.template.contracts;

import com.template.states.LoanRequestState;
import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * @author RaviTeja.l
 */
public class RequestToBankContract implements Contract {


    public static final String REQUEST_TO_BANK_CONTRACT_ID = "com.template.contracts.RequestToBankContract";
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        if (tx != null && tx.getCommands().size() != 1) {
            throw new IllegalArgumentException("Transaction must have one command");
        }
        Command command = tx.getCommand(0);
        List<PublicKey> requiredSigners = command.getSigners();
        CommandData commandType = command.getValue();

        if (commandType instanceof Commands.RequestForLoan) {
            verifyRequestForLoan(tx,requiredSigners);

        } else if (commandType instanceof Commands.ResponseFromBank) {
            verifyResponseFromBank(tx,requiredSigners);
        }
    }

    private void verifyRequestForLoan(LedgerTransaction lTx,List<PublicKey> signers){
        requireThat(req -> {

            req.using("No input should be consumed while initiating loan", lTx.getInputStates().isEmpty());
            req.using("Only one output should be created during the process of initiating loan", lTx.getOutputStates().size() == 1);

            ContractState outputState = lTx.getOutput(0);

            req.using(" Ouput must be a LoanRequestState", outputState instanceof LoanRequestState);

            LoanRequestState loanReqState = (LoanRequestState) outputState;

            req.using("Loan amount should not be zero", loanReqState.getAmount() > 0);

            Party dealer = loanReqState.getDealer();

            PublicKey dealerKey = dealer.getOwningKey();
            PublicKey bankKey = loanReqState.getBank().getOwningKey();

            req.using("Dealer should sign the transaction", signers.contains(dealerKey));

            req.using("Bank should sign the transaction", signers.contains(bankKey));

            return null;
        });
    }

    private void verifyResponseFromBank(LedgerTransaction lTx,List<PublicKey> signers){
        requireThat(req -> {

            req.using("Only one input should be consumed while giving loan application response to dealer agency", lTx.getInputStates().size() == 1);
            req.using("Only one output should be created ", lTx.getOutputStates().size() == 1);

            ContractState input = lTx.getInput(0);
            ContractState output = lTx.getOutput(0);

            req.using("input must be of type LoanRequestState ", input instanceof LoanRequestState);
            req.using("output must be of the type LoanRequestState ", output instanceof LoanRequestState);

            LoanRequestState inputState = (LoanRequestState) input;
            LoanRequestState outputState = (LoanRequestState) output;

            PublicKey bankKey = inputState.getBank().getOwningKey();
            PublicKey dealerKey = outputState.getDealer().getOwningKey();

            req.using("Dealer should sign the transaction", signers.contains(dealerKey));
            req.using("Bank should sign the transaction", signers.contains(bankKey));

            return null;
        });
    }

    public interface Commands extends CommandData {
        class RequestForLoan implements Commands { }
        class ResponseFromBank implements Commands { }
    }
}
