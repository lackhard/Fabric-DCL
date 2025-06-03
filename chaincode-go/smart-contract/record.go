package abac

import (
	"chaincode-go/model"
	"encoding/json"
	"fmt"
	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

//访问记录相关
func (s *SmartContract) CreateRecord(ctx contractapi.TransactionContextInterface, request string) error {

	var record model.Record
	err := json.Unmarshal([]byte(request), &record)
	if err != nil {
		return err
	}
	//构建真正的id
	record.Id = fmt.Sprintf("record:%s:%s", record.ResourceId, record.Id)
	return ctx.GetStub().PutState(record.Id, []byte(request))
}

// 通过resourceid 获取全部的访问记录
func (s *SmartContract) GetRecordsByResourceId(ctx contractapi.TransactionContextInterface, id string) ([]*model.Record, error) {

	startKey := fmt.Sprintf("record:%s", id)
	endKey := string(BytesPrefix([]byte(startKey)))
	resultsIterator, err := ctx.GetStub().GetStateByRange(startKey, endKey)

	if err != nil {
		return nil, err
	}
	defer resultsIterator.Close()

	var resources []*model.Record
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			return nil, err
		}

		var record model.Record

		err = json.Unmarshal(queryResponse.Value, &record)
		if err != nil {
			return nil, err
		}
		resources = append(resources, &record)
	}

	return resources, nil
}
