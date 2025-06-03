package abac

import (
	"github.com/hyperledger/fabric-contract-api-go/contractapi"
	"strconv"
)

//put key value
func (s *SmartContract) Put(ctx contractapi.TransactionContextInterface, key string, value string) error {

	return ctx.GetStub().PutState(key, []byte(value))
}

// Get
func (s *SmartContract) Get(ctx contractapi.TransactionContextInterface, key string) (string, error) {

	res, err := ctx.GetStub().GetState(key)
	return string(res), err
}

// 对一个key 进行增加值
func (s *SmartContract) AddValue(ctx contractapi.TransactionContextInterface, key string, value string) error {
	originState, err := ctx.GetStub().GetState(key)
	if err != nil {
		return err
	}
	increment, err := strconv.Atoi(value)
	if err != nil {
		return err
	}
	state, err := strconv.Atoi(string(originState))
	state += increment

	return ctx.GetStub().PutState(key, []byte(strconv.Itoa(state)))
}
