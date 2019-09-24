package com.template;

import com.google.common.collect.ImmutableList;
import com.template.flows.RequestBankFlow;
import com.template.flows.ResponseBankFlow;
import com.template.states.LoanRequestState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class FlowTests {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
        TestCordapp.findCordapp("com.template.contracts"),
        TestCordapp.findCordapp("com.template.flows")
    )));
    private final StartedMockNode dealer = network.createNode();
    private final StartedMockNode bank = network.createNode();
    private static int amount = -1;
    private static String companyName = "Varun Hyundai";

    public FlowTests() {
        dealer.registerInitiatedFlow(RequestBankFlow.RequestToBank.class);
        bank.registerInitiatedFlow(ResponseBankFlow.Initiator.class);
    }

    @Before
    public void setup() {
        network.runNetwork();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutputTheInputIOU() throws Exception {
        RequestBankFlow.RequestToBank flow = new RequestBankFlow.RequestToBank( bank.getInfo().getLegalIdentities().get(0), amount,companyName);
        CordaFuture<SignedTransaction> future = dealer.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(dealer, bank)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert (((List) txOutputs).size() == 1);

            LoanRequestState recordedState = (LoanRequestState) txOutputs.get(0).getData();
            assertEquals(recordedState.getDealer(), dealer.getInfo().getLegalIdentities().get(0));
            assertEquals(recordedState.getBank(), bank.getInfo().getLegalIdentities().get(0));
        }
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        RequestBankFlow.RequestToBank flow = new RequestBankFlow.RequestToBank( bank.getInfo().getLegalIdentities().get(0), amount,companyName);
        CordaFuture<SignedTransaction> future = dealer.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(dealer, bank)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        RequestBankFlow.RequestToBank flow = new RequestBankFlow.RequestToBank( bank.getInfo().getLegalIdentities().get(0), amount,companyName);
        CordaFuture<SignedTransaction> future = dealer.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(dealer.getInfo().getLegalIdentities().get(0).getOwningKey());
    }


    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        RequestBankFlow.RequestToBank flow = new RequestBankFlow.RequestToBank( bank.getInfo().getLegalIdentities().get(0), amount,companyName);
        CordaFuture<SignedTransaction> future = dealer.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(bank.getInfo().getLegalIdentities().get(0).getOwningKey());
    }
}
