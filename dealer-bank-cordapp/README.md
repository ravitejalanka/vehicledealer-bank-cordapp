# The Finance-Loan Application

This simple CorDapp provides the facility for un-trusting parties to interact and transact with one another to reach a final state of consensus where all parties can trust in the outcome without trusting one another and without the need for expensive out of band reconciliation.
This application shows how Dealer(Borrower), Banks can use corda DLT for Loan lending process.This corDapp has 3 parties 

1. Dealer (A  party who raises a loan request)
2. Banks (Bank verifies and lends/approves the loan application)

## CorDapp structure
*	There are 3 Parties and 1 Notary.
* There is Linear states 1) LoanRequestState
*	Party A and B are Vehicle Dealers which sends the loan application to the bank 
*	Party B is a Bank which Lends/Approves the loan 

## CorDapp flow
*	Dealer sends the loan application to the bank which contains individual's or company's name and the loan amount.
*	Bank will receive the application and validates  and approve or reject the loan application. 
*	Use API endpoints to initiate flow using REST API.

## Minimum System Requirements
* 16 GB RAM preferably
* Latest version of JAVA 8 java 8u181 (Preferably, Corda and kotlin support latest version of java 8)
* http://docs.corda.r3.com/sizing-and-performance.html 

## Instructions for setting up
1. clone the repository https://github.com/ravitejalanka/vehicledealer-bank-cordapp
2. To build on unix : ./gradlew deployNodes
3. To build on windows : gradlew.bat deployNodes
4. For running corDapp on unix ./runnodes --log-to-console --logging-level=DEBUG
5. For running corDapp on windows. Go to $project_dir\workflows\build\nodes runnodes.bat --log-to-console --logging-level=DEBUG


## Accessing over API endpoints 

| Node                  |    Port         |
| --------------------- | --------------- | 
| Maruthi Dealer         | localhost:10055 |
| Hyundai Dealer                  | localhost:10060 |      
| Bank  | localhost:10050 |   





