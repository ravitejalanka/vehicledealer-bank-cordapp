package com.template;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.utilities.NetworkHostAndPort;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.driver.WebserverHandle;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;

import java.util.List;

import static net.corda.testing.driver.Driver.driver;
import static org.junit.Assert.assertEquals;

public class DriverBasedTest {
    private final TestIdentity bankA = new TestIdentity(new CordaX500Name("BankA", "", "GB"));
    private final TestIdentity bankB = new TestIdentity(new CordaX500Name("BankB", "", "US"));

    @Test
    public void nodeTest() {
        driver(new DriverParameters().withIsDebug(true).withStartNodesInProcess(true), dsl -> {
            // Start a pair of nodes and wait for them both to be ready.
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(bankA.getName())),
                    dsl.startNode(new NodeParameters().withProvidedName(bankB.getName()))
            );

            try {
                NodeHandle partyAHandle = handleFutures.get(0).get();
                NodeHandle partyBHandle = handleFutures.get(1).get();

                // From each node, make an RPC call to retrieve another node's name from the network map, to verify that the
                // nodes have started and can communicate.

                // This is a very basic test: in practice tests would be starting flows, and verifying the states in the vault
                // and other important metrics to ensure that your CorDapp is working as intended.
                assertEquals(partyAHandle.getRpc().wellKnownPartyFromX500Name(bankB.getName()).getName(), bankB.getName());
                assertEquals(partyBHandle.getRpc().wellKnownPartyFromX500Name(bankA.getName()).getName(), bankA.getName());
            } catch (Exception e) {
                throw new RuntimeException("Caught exception during test: ", e);
            }

            return null;
        });
    }

    @Test
    public void nodeWebserverTest() {
        driver(new DriverParameters().withIsDebug(true).withStartNodesInProcess(true), dsl -> {

            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(bankA.getName())),
                    dsl.startNode(new NodeParameters().withProvidedName(bankB.getName()))
            );

            try {
                // This test starts each node's webserver and makes an HTTP call to retrieve the body of a GET endpoint on
                // the node's webserver, to verify that the nodes' webservers have started and have loaded the API.
                for (CordaFuture<NodeHandle> handleFuture : handleFutures) {
                    NodeHandle nodeHandle = handleFuture.get();

                    WebserverHandle webserverHandle = dsl.startWebserver(nodeHandle).get();

                    NetworkHostAndPort nodeAddress = webserverHandle.getListenAddress();
                    String url = String.format("http://%s/loan-transfer/states", nodeAddress);

                    Request request = new Request.Builder().url(url).build();
                    OkHttpClient client = new OkHttpClient();
                    Response response = client.newCall(request).execute();

                    assertEquals("[ ]", response.body().string());
                }
            } catch (Exception e) {
                throw new RuntimeException("Caught exception during test", e);
            }

            return null;
        });
    }
}