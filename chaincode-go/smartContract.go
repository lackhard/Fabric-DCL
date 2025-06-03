package main

import (
	abac "chaincode-go/smart-contract"
	"github.com/hyperledger/fabric-contract-api-go/contractapi"
	"log"
)

func main() {
	abacSmartContract, err := contractapi.NewChaincode(&abac.SmartContract{})
	if err != nil {
		log.Panicf("Error creating abac chaincode: %v", err)
	}

	if err := abacSmartContract.Start(); err != nil {
		log.Panicf("Error starting abac chaincode: %v", err)
	}
}
