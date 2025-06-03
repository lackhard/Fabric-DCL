package abac

import (
	"chaincode-go/model"
	"encoding/json"
	"fmt"
	"github.com/hyperledger/fabric-contract-api-go/contractapi"
	"log"
)

/**
资源
*/
func (s *SmartContract) CreateResource(ctx contractapi.TransactionContextInterface, request string) error {

	//// Get ID of submitting client identity
	clientID, err := s.GetSubmittingClientIdentity(ctx)

	if err != nil {
		return fmt.Errorf("获取用户id失败")
	}

	var resource model.Resource
	err = json.Unmarshal([]byte(request), &resource)

	resource.Owner = clientID
	exists, err := s.ResourceExists(ctx, resource.Id)
	resource.Controllers = []string{clientID}

	if err != nil {
		return err
	}
	if exists {
		return fmt.Errorf("the resource %s already exists", resource.Id)
	}

	resourceJSON, err := json.Marshal(resource)
	if err != nil {
		return err
	}

	return ctx.GetStub().PutState(resource.Id, resourceJSON)
}

//查询资源
func (s *SmartContract) FindResourceById(ctx contractapi.TransactionContextInterface, resourceId string) (*model.Resource, error) {
	log.Printf("要查找的资源id:%v", resourceId)
	resourceAsByte, err := ctx.GetStub().GetState(resourceId)
	if err != nil {
		return nil, fmt.Errorf("查询资源失败")
	}
	var resource model.Resource
	err = json.Unmarshal(resourceAsByte, &resource)

	return &resource, err
}

//删除资源
func (s *SmartContract) DeleteResourceById(ctx contractapi.TransactionContextInterface, resourceId string) (string, error) {

	err := ctx.GetStub().DelState(resourceId)
	if err != nil {
		return "删除资源失败", fmt.Errorf("删除资源失败")
	}

	return "删除资源成功", nil
}
func (s *SmartContract) ResourceExists(ctx contractapi.TransactionContextInterface, id string) (bool, error) {

	resource, err := ctx.GetStub().GetState(id)
	if err != nil {
		return false, fmt.Errorf("failed to read from world state: %v", err)
	}

	return resource != nil, nil
}

// returns all resource found in world state
func (s *SmartContract) GetAllResource(ctx contractapi.TransactionContextInterface) ([]*model.Resource, error) {

	startKey := "resource:"
	endKey := string(BytesPrefix([]byte(startKey)))
	resultsIterator, err := ctx.GetStub().GetStateByRange(startKey, endKey)

	if err != nil {
		return nil, err
	}
	defer resultsIterator.Close()

	var resources []*model.Resource
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			return nil, err
		}

		var resource model.Resource

		err = json.Unmarshal(queryResponse.Value, &resource)
		if err != nil {
			return nil, err
		}
		resources = append(resources, &resource)
	}

	return resources, nil
}

//add resource controller
func (s *SmartContract) AddResourceController(ctx contractapi.TransactionContextInterface, request string) (bool, error) {
	var requestObj model.AddResourceControllerRequest
	err := json.Unmarshal([]byte(request), &requestObj)
	if err != nil {
		return false, fmt.Errorf("反序列化请求失败")
	}
	identity, err := s.GetSubmittingClientIdentity(ctx)
	if err != nil {
		return false, fmt.Errorf("获取提交者身份异常")
	}
	resource, err := s.FindResourceById(ctx, requestObj.ResourceId)
	if err != nil {
		return false, fmt.Errorf("查找资源失败")
	}
	if identity != resource.Owner {
		return false, fmt.Errorf("抱歉，您不是该资源的拥有者")
	}
	resource.Controllers = append(resource.Controllers, requestObj.ControllerId)
	reourceByte, err := json.Marshal(resource)
	if err != nil {
		return false, fmt.Errorf("序列化失败")
	}
	ctx.GetStub().PutState(resource.Id, reourceByte)
	return true, nil
}
