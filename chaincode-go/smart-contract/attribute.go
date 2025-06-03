package abac

import (
	"chaincode-go/model"
	"encoding/json"
	"fmt"
	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

//属性相关

// 增加公有属性
func (s *SmartContract) AddAttribute(ctx contractapi.TransactionContextInterface, request string) error {

	clientID, err := s.GetSubmittingClientIdentity(ctx)

	if err != nil {
		return err
	}

	var attribute model.Attribute
	err = json.Unmarshal([]byte(request), &attribute)
	if err != nil {
		return err
	}

	//判断属性是否存在
	if attributeExist(ctx, attribute, clientID) {
		return fmt.Errorf("该属性已经存在")
	}
	//判断是否 增加公有属性 还是私有属性
	if attribute.Type == "PUBLIC" {
		id := fmt.Sprintf("attribute:public:%s:%s", clientID, attribute.Key)
		attribute.Id = id

		//以json的形式放回去
		attributeBytes, err := json.Marshal(attribute)
		if err != nil {
			fmt.Errorf("json 序列化失败")
		}
		ctx.GetStub().PutState(id, attributeBytes)
	} else {
		//私有属性
		return fmt.Errorf("add private attribute")
	}
	return err
}

// 发布私有属性
func (s *SmartContract) PublishPrivateAttribute(ctx contractapi.TransactionContextInterface, request string) (res string, err error) {

	clientID, err := s.GetSubmittingClientIdentity(ctx)

	res = ""

	var attribute model.Attribute
	err = json.Unmarshal([]byte(request), &attribute)

	//判断是否 增加公有属性 还是私有属性
	if attribute.Type == "PRIVATE" {
		// 判断该资源是否存在
		exist, _ := s.ResourceExists(ctx, attribute.ResourceId)

		if !exist {
			err = fmt.Errorf("要增加的私有属性 该资源不存在")
			return
		}
		//直接放进去
		attribute.Owner = clientID
		//fmt.Sprintf("attribute:private:resourceid:key")
		attributeId := fmt.Sprintf("attribute:private:%s:%s", attribute.ResourceId, attribute.Key)
		attribute.Id = attributeId
		attributeAsJsonByte, _ := json.Marshal(attribute)

		ctx.GetStub().PutState(attribute.Id, attributeAsJsonByte)
		res = attributeId
		return
	} else {
		//公有属性
		err = fmt.Errorf("can only add private attribute")
		return
	}

}

//查询属性
func (s *SmartContract) FindAttributeById(ctx contractapi.TransactionContextInterface, attributeId string) (*model.Attribute, error) {

	attributeAsByte, err := ctx.GetStub().GetState(attributeId)
	if err != nil {
		return nil, fmt.Errorf("查询属性失败")
	}
	var attribute model.Attribute
	err = json.Unmarshal(attributeAsByte, &attribute)

	return &attribute, err
}

//查看某资源对应的 可买属性
func (s *SmartContract) FindAttributeByResourceId(ctx contractapi.TransactionContextInterface, resourceId string) ([]*model.Attribute, error) {
	//attribute:private:resourceid:key
	startKey := "attribute:private:" + resourceId + ":"
	endKey := string(BytesPrefix([]byte(startKey)))
	resultsIterator, err := ctx.GetStub().GetStateByRange(startKey, endKey)

	if err != nil {
		return nil, err
	}
	defer resultsIterator.Close()

	var attributes []*model.Attribute
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			return nil, err
		}

		var attribute model.Attribute

		err = json.Unmarshal(queryResponse.Value, &attribute)
		if err != nil {
			return nil, err
		}
		attributes = append(attributes, &attribute)
	}

	return attributes, nil

}

// 购买私有属性
func (s *SmartContract) BuyPrivateAttribute(ctx contractapi.TransactionContextInterface, request string) error {

	clientID, err := s.GetSubmittingClientIdentity(ctx)

	if err != nil {
		return err
	}

	var buyAttributeRequest model.BuyAttributeRequest
	err = json.Unmarshal([]byte(request), &buyAttributeRequest)
	if err != nil {
		return err
	}
	//验证买家的身份
	if clientID != buyAttributeRequest.Buyer {
		return fmt.Errorf("您的身份不匹配")
	}

	//查找出 买家 和卖家
	buyer, err := s.FindUserById(ctx, clientID)
	//
	seller, err := s.FindUserById(ctx, buyAttributeRequest.Seller)

	//根据属性id 查询属性
	attribute, err := s.FindAttributeById(ctx, buyAttributeRequest.AttributeId)

	//验证该属性的资源是否是卖家拥有
	resource, err := s.FindResourceById(ctx, attribute.ResourceId)

	if seller.ID != resource.Owner {
		return fmt.Errorf("该资源不是该卖家所有")
	}

	////转账
	seller.Money += attribute.Money
	buyer.Money -= attribute.Money

	//增加购买者的私有属性
	//attribute:private:userid:resourceid:key
	levelDbId := fmt.Sprintf("attribute:private:%s:%s:%s", buyer.ID, attribute.ResourceId, attribute.Key)
	//buyer.Attributes = append(seller.Attributes, *attribute)
	attributeAsBytes, err := json.Marshal(attribute)
	if err != nil {
		return fmt.Errorf("序列化失败")
	}
	ctx.GetStub().PutState(levelDbId, attributeAsBytes)
	////分别存储buyer 和seller
	buyerAsJsonByte, err := json.Marshal(buyer)
	sellerAsJsonByte, err := json.Marshal(seller)

	ctx.GetStub().PutState(seller.ID, sellerAsJsonByte)
	ctx.GetStub().PutState(buyer.ID, buyerAsJsonByte)
	return err
}

//根据userId查找用户的所有属性（公有属性+私有属性）
func (s *SmartContract) FindAttributeByUserId(ctx contractapi.TransactionContextInterface, userid string) []model.Attribute {
	var attributes []model.Attribute
	//检索该用户公有属性 attribute:public:userid:key
	startKey := "attribute:public:" + userid + ":"
	endKey := string(BytesPrefix([]byte(startKey)))
	publicIterator, err := ctx.GetStub().GetStateByRange(startKey, endKey)
	if err != nil {
		return nil
	}
	defer publicIterator.Close()
	for publicIterator.HasNext() {
		publicAttribute, err := publicIterator.Next()
		if err != nil {
			return nil
		}
		var attribute model.Attribute
		json.Unmarshal(publicAttribute.Value, &attribute)
		attributes = append(attributes, attribute)
	}

	//检索私有属性
	//attribute:private:userid:resourceid:key
	startKey = "attribute:private:" + userid + ":"
	endKey = string(BytesPrefix([]byte(startKey)))
	privateIterator, err := ctx.GetStub().GetStateByRange(startKey, endKey)
	if err != nil {
		return nil
	}
	defer privateIterator.Close()
	for privateIterator.HasNext() {
		privateIterator, err := privateIterator.Next()
		if err != nil {
			return nil
		}
		var attribute model.Attribute
		json.Unmarshal(privateIterator.Value, &attribute)
		attributes = append(attributes, attribute)
	}
	return attributes
}

//判断该用户是否具有该属性
//attribute:public:userid:key
// attribute:private:resourceid:key
// attribute:private:userid:resourceid:key
func attributeExist(ctx contractapi.TransactionContextInterface, attribute model.Attribute, userId string) bool {
	res := false
	id := ""
	if attribute.Type == "PUBLIC" {
		id = "attribute:public:" + userId + ":" + attribute.Key
	} else if attribute.Type == "PRIVATE" {
		id = "attribute:private:" + userId + ":" + attribute.ResourceId + ":" + attribute.Key
	}
	state, err := ctx.GetStub().GetState(id)
	if err != nil {
		fmt.Errorf("获取key失败")
		return false
	}
	if state != nil && len(state) > 0 {
		res = true
	}
	return res
}
