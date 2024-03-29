package com.template.webserver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.template.flows.RequestBankFlow;
import com.template.flows.ResponseBankFlow;
import com.template.pojo.LoanRequest;
import com.template.pojo.LoanResponse;
import com.template.states.LoanRequestState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.print.attribute.standard.Media;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author RaviTeja.l
 */
@RestController
@RequestMapping("/loan-transfer")
public class Controller {
    private final CordaRPCOps proxy;
    private final CordaX500Name myLegalName;

    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.myLegalName = proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, CordaX500Name> whoami() {
        return ImmutableMap.of("me", myLegalName);
    }

    @GetMapping(value = "/loanRequestStates", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<LoanRequestState>>> getLoanRequestStates() {
        System.out.println("VaultQuery : " + proxy.vaultQuery(LoanRequestState.class).getStates());
        return ResponseEntity.ok(proxy.vaultQuery(LoanRequestState.class).getStates());
    }

    @PostMapping(value = "/loan-request", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity loanRequest(LoanRequest loanRequest) {

        String bank = loanRequest.getParty();
        int amount = Integer.valueOf(loanRequest.getAmount());
        String dealerCompanyName = loanRequest.getDealerName();
        CordaX500Name bankParty = CordaX500Name.parse(bank);
        final Party otherParty = proxy.wellKnownPartyFromX500Name(bankParty);

        if (bank == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("parameter 'party' missing or has wrong format.\n");
        }

        if (amount <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(" parameter 'Amount' must be non-negative.\n");
        }
        if (dealerCompanyName==null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(" parameter 'DealerName' is missing.\n");
        }

        if (otherParty == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Party named " + bank + "cannot be found.\n");
        }

        try {
            RequestBankFlow.RequestToBank requestBank = new RequestBankFlow.RequestToBank(otherParty, amount,dealerCompanyName);
            final SignedTransaction signedTx = proxy
                    .startTrackedFlowDynamic(requestBank.getClass(), otherParty, amount,dealerCompanyName)
                    .getReturnValue()
                    .get();

            final String msg = String.format("Car Dealer with \n Transaction id %s  is successfully committed to ledger.\n ", signedTx.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(msg);
        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
        }
    }


    @PostMapping(value = "/loan-response", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity loanResponse(LoanResponse loanResponse) {
        String dealer = loanResponse.getParty();
        CordaX500Name partyName = CordaX500Name.parse(dealer);
        String loanRequestLinearId = loanResponse.getLinearId();
        final Party otherParty = proxy.wellKnownPartyFromX500Name(partyName);

        if (partyName == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(" parameter 'partyName' missing or has wrong format.\n");
        }

        if (otherParty == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Party named " + partyName + "cannot be found.\n");
        }

        UniqueIdentifier linearIdLoanRequestState = new UniqueIdentifier();
        UniqueIdentifier uuidLoanReqState = linearIdLoanRequestState.copy(null, UUID.fromString(loanRequestLinearId));

        try {
            ResponseBankFlow.Initiator initiator = new ResponseBankFlow.Initiator(otherParty, uuidLoanReqState);
            final SignedTransaction signedTx = proxy
                    .startTrackedFlowDynamic(initiator.getClass(), otherParty, uuidLoanReqState)
                    .getReturnValue()
                    .get();

            final String msg = String.format("Loan Request Response from BANK. \n Transaction id %s is sucessfully  committed to ledger.\n", signedTx.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(msg);

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
        }
    }
}
