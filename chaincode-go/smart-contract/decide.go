package abac

import (
	"chaincode-go/model"
	"encoding/json"
	"fmt"
	"github.com/hyperledger/fabric-contract-api-go/contractapi"
	"log"
	"strconv"
	"time"
)

//计算GetStateByRange时的endKey，该函数摘自：github.com/syndtr/goleveldb/leveldb/util
func BytesPrefix(prefix []byte) []byte {
	var limit []byte
	for i := len(prefix) - 1; i >= 0; i-- {
		c := prefix[i]
		if c < 0xff {
			limit = make([]byte, i+1)
			copy(limit, prefix)
			limit[i] = c + 1
			break
		}
	}
	return limit
}

//策略决策部分 不写入访问记录
func (s *SmartContract) DecideNoRecord(ctx contractapi.TransactionContextInterface, request string) (string, error) {
	//1. 查询资源
	//2. 根据资源取回策略
	//3. 查询策略中需要的主体属性和客体属性
	//4. 根据策略进行决策

	// 先来个简易版
	var decideRequest model.DecideRequest
	err := json.Unmarshal([]byte(request), &decideRequest)
	if err != nil {
		return "false", err
	}
	//log.Printf("decideRequest:%v", decideRequest)
	//查询资源
	resourceAsByte, err := ctx.GetStub().GetState(decideRequest.ResourceId)
	if err != nil {
		return "查询资源失败", fmt.Errorf("查询资源失败")
	}
	var resource model.Resource
	err = json.Unmarshal(resourceAsByte, &resource)
	//resource, err := s.FindResourceById(ctx, decideRequest.ResourceId)
	//log.Printf("resource:%v", resource)
	//log.Printf("resource2:%v", *resource)
	//log.Printf("resource controllers:%v", resource.Controllers)
	//验证资源控制器身份
	controllerId, err := s.GetSubmittingClientIdentity(ctx)
	controllers := resource.Controllers
	i := 0
	for i < len(controllers) {
		log.Printf("controllerId: %v", controllerId)
		log.Printf("currentId: %v", controllers[i])
		if controllers[i] == controllerId {
			log.Printf("当前资源控制器身份验证通过")
			break
		}
		i++
	}
	if i == len(controllers) {
		log.Printf("i=%v\n", i)
		return "false", fmt.Errorf("资源控制器身份验证不通过")
	}

	attributeMap := make(map[string]interface{})
	//获取主体的公有属性
	publicAttributeStartKey := fmt.Sprintf("attribute:public:%s:", decideRequest.RequesterId)
	publicAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	publicAttributeRange, err := ctx.GetStub().GetStateByRange(publicAttributeStartKey, publicAttributeEndKey)
	if err != nil {
		return "", fmt.Errorf("获取公有属性失败")
	}
	defer publicAttributeRange.Close()
	for publicAttributeRange.HasNext() {
		result, err := publicAttributeRange.Next()
		if err != nil {
			return "", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		attributeMap[attribute.Key] = attribute.Value
	}
	//获取私有属性 attribute:private:userid:resourceid:key
	privateAttributeStartKey := fmt.Sprintf("attribute:private:%s:%s:", decideRequest.RequesterId, decideRequest.ResourceId)
	privateAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	privateAttributeRange, err := ctx.GetStub().GetStateByRange(privateAttributeStartKey, privateAttributeEndKey)
	if err != nil {
		return "", fmt.Errorf("获取公有属性失败")
	}
	defer privateAttributeRange.Close()
	for privateAttributeRange.HasNext() {
		result, err := privateAttributeRange.Next()
		if err != nil {
			return "", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		attributeMap[attribute.Key] = attribute.Value
	}

	//根据资源查找对应的策略
	//这里暂时写死一个资源的策略
	if attributeMap["age"] == "40" && attributeMap["occupation"] == "doctor" {
		return "true", nil
	}
	return "false", nil

}

//策略决策部分 写入访问记录
func (s *SmartContract) DecideWithRecord(ctx contractapi.TransactionContextInterface, request string) (string, error) {
	//1. 查询资源
	//2. 根据资源取回策略
	//3. 查询策略中需要的主体属性和客体属性
	//4. 根据策略进行决策

	// 先来个简易版
	var decideRequest model.DecideRequest
	err := json.Unmarshal([]byte(request), &decideRequest)
	if err != nil {
		return "请求序反序列化失败false", err
	}

	//查询资源
	//查询资源
	resourceAsByte, err := ctx.GetStub().GetState(decideRequest.ResourceId)
	if err != nil {
		return "查询资源失败", fmt.Errorf("查询资源失败")
	}
	var resource model.Resource
	err = json.Unmarshal(resourceAsByte, &resource)

	//验证资源控制器身份
	controllerId, err := s.GetSubmittingClientIdentity(ctx)
	controllers := resource.Controllers
	i := 0
	for i < len(controllers) {
		if controllers[i] == controllerId {
			break
		}
		i++
	}
	if i == len(controllers) {
		return "资源控制器身份验证不通过,false", fmt.Errorf("资源控制器身份验证不通过")
	}
	//验证请求者身份

	//requesterId, err := s.FindUserById(ctx, decideRequest.RequesterId)
	//if err != nil {
	//	return "false", err
	//}

	attributeMap := make(map[string]interface{})
	//获取主体的公有属性
	publicAttributeStartKey := fmt.Sprintf("attribute:public:%s:", decideRequest.RequesterId)
	publicAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	publicAttributeRange, err := ctx.GetStub().GetStateByRange(publicAttributeStartKey, publicAttributeEndKey)
	if err != nil {
		return "获取公有属性失败", fmt.Errorf("获取公有属性失败")
	}
	defer publicAttributeRange.Close()
	for publicAttributeRange.HasNext() {
		result, err := publicAttributeRange.Next()
		if err != nil {
			return "查询公有属性是该", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		attributeMap[attribute.Key] = attribute.Value
	}
	//获取私有属性 attribute:private:userid:resourceid:key
	privateAttributeStartKey := fmt.Sprintf("attribute:private:%s:%s:", decideRequest.RequesterId, decideRequest.ResourceId)
	privateAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	privateAttributeRange, err := ctx.GetStub().GetStateByRange(privateAttributeStartKey, privateAttributeEndKey)
	if err != nil {
		return "获取私有属性失败", fmt.Errorf("获取私有属性失败")
	}
	defer privateAttributeRange.Close()
	for privateAttributeRange.HasNext() {
		result, err := privateAttributeRange.Next()
		if err != nil {
			return "获取私有属性失败", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		attributeMap[attribute.Key] = attribute.Value
	}

	//根据资源查找对应的策略
	//这里暂时写死一个资源的策略
	if attributeMap["age"] == "40" && attributeMap["occupation"] == "doctor" {
		var record model.Record
		record.Id = decideRequest.Id
		record.RequesterId = decideRequest.RequesterId
		record.ResourceId = decideRequest.ResourceId
		record.Response = "true"
		recordJsonAsByte, _ := json.Marshal(record)
		err := s.CreateRecord(ctx, string(recordJsonAsByte))
		if err != nil {
			return "", err
		}
		return "true", nil
	}

	return "false", nil

}

//策略决策部分 不写入访问记录 使用4个属性
func (s *SmartContract) DecideNoRecord4Attributes(ctx contractapi.TransactionContextInterface, request string) (string, error) {
	//1. 查询资源
	//2. 根据资源取回策略
	//3. 查询策略中需要的主体属性和客体属性
	//4. 根据策略进行决策

	// 先来个简易版
	var decideRequest model.DecideRequest
	err := json.Unmarshal([]byte(request), &decideRequest)
	if err != nil {
		return "false", err
	}
	//log.Printf("decideRequest:%v", decideRequest)
	//查询资源
	resourceAsByte, err := ctx.GetStub().GetState(decideRequest.ResourceId)
	if err != nil {
		return "查询资源失败", fmt.Errorf("查询资源失败")
	}
	var resource model.Resource
	err = json.Unmarshal(resourceAsByte, &resource)
	//resource, err := s.FindResourceById(ctx, decideRequest.ResourceId)
	//log.Printf("resource:%v", resource)
	//log.Printf("resource2:%v", *resource)
	//log.Printf("resource controllers:%v", resource.Controllers)
	//验证资源控制器身份
	controllerId, err := s.GetSubmittingClientIdentity(ctx)
	controllers := resource.Controllers
	i := 0
	for i < len(controllers) {
		log.Printf("controllerId: %v", controllerId)
		log.Printf("currentId: %v", controllers[i])
		if controllers[i] == controllerId {
			log.Printf("当前资源控制器身份验证通过")
			break
		}
		i++
	}
	if i == len(controllers) {
		log.Printf("i=%v\n", i)
		return "false", fmt.Errorf("资源控制器身份验证不通过")
	}

	publicAttributeMap := make(map[string]model.Attribute)
	privateAttributeMap := make(map[string]model.Attribute)
	//获取主体的公有属性
	publicAttributeStartKey := fmt.Sprintf("attribute:public:%s:", decideRequest.RequesterId)
	publicAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	publicAttributeRange, err := ctx.GetStub().GetStateByRange(publicAttributeStartKey, publicAttributeEndKey)
	if err != nil {
		return "", fmt.Errorf("获取公有属性失败")
	}
	defer publicAttributeRange.Close()
	for publicAttributeRange.HasNext() {
		result, err := publicAttributeRange.Next()
		if err != nil {
			return "", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		publicAttributeMap[attribute.Key] = attribute
	}
	//获取私有属性 attribute:private:userid:resourceid:key
	privateAttributeStartKey := fmt.Sprintf("attribute:private:%s:%s:", decideRequest.RequesterId, decideRequest.ResourceId)
	privateAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	privateAttributeRange, err := ctx.GetStub().GetStateByRange(privateAttributeStartKey, privateAttributeEndKey)
	if err != nil {
		return "", fmt.Errorf("获取私有属性失败")
	}
	defer privateAttributeRange.Close()
	for privateAttributeRange.HasNext() {
		result, err := privateAttributeRange.Next()
		if err != nil {
			return "", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		privateAttributeMap[attribute.Key] = attribute
	}
	//根据资源查找对应的策略
	//这里暂时写死一个资源的策略

	//currentTimeInt64 := time.Now().Unix()*1000
	currentTimeInt64 := time.Now().Unix() * 1000
	currentTime := strconv.FormatInt(currentTimeInt64, 10)
	if publicAttributeMap["age"].Value == "40" && privateAttributeMap["occupation"].Value == "doctor" &&
		publicAttributeMap["ip"].Value == "192.168.2.1" && currentTime >= "1669791474807" {
		return "true", nil
	}
	return "false", nil

}

//策略决策部分 写入访问记录
func (s *SmartContract) DecideWithRecord4Attributes(ctx contractapi.TransactionContextInterface, request string) (string, error) {
	//1. 查询资源
	//2. 根据资源取回策略
	//3. 查询策略中需要的主体属性和客体属性
	//4. 根据策略进行决策

	// 先来个简易版
	var decideRequest model.DecideRequest
	err := json.Unmarshal([]byte(request), &decideRequest)
	if err != nil {
		return "请求序反序列化失败false", err
	}

	//查询资源
	//查询资源
	resourceAsByte, err := ctx.GetStub().GetState(decideRequest.ResourceId)
	if err != nil {
		return "查询资源失败", fmt.Errorf("查询资源失败")
	}
	var resource model.Resource
	err = json.Unmarshal(resourceAsByte, &resource)

	//验证资源控制器身份
	controllerId, err := s.GetSubmittingClientIdentity(ctx)
	controllers := resource.Controllers
	i := 0
	for i < len(controllers) {
		if controllers[i] == controllerId {
			break
		}
		i++
	}
	if i == len(controllers) {
		return "资源控制器身份验证不通过,false", fmt.Errorf("资源控制器身份验证不通过")
	}

	publicAttributeMap := make(map[string]model.Attribute)
	privateAttributeMap := make(map[string]model.Attribute)
	//获取主体的公有属性
	publicAttributeStartKey := fmt.Sprintf("attribute:public:%s:", decideRequest.RequesterId)
	publicAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	publicAttributeRange, err := ctx.GetStub().GetStateByRange(publicAttributeStartKey, publicAttributeEndKey)
	if err != nil {
		return "获取公有属性失败", fmt.Errorf("获取公有属性失败")
	}
	defer publicAttributeRange.Close()
	for publicAttributeRange.HasNext() {
		result, err := publicAttributeRange.Next()
		if err != nil {
			return "查询公有属性是该", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		publicAttributeMap[attribute.Key] = attribute
	}
	//获取私有属性 attribute:private:userid:resourceid:key
	privateAttributeStartKey := fmt.Sprintf("attribute:private:%s:%s:", decideRequest.RequesterId, decideRequest.ResourceId)
	privateAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	privateAttributeRange, err := ctx.GetStub().GetStateByRange(privateAttributeStartKey, privateAttributeEndKey)
	if err != nil {
		return "获取私有属性失败", fmt.Errorf("获取私有属性失败")
	}
	defer privateAttributeRange.Close()
	for privateAttributeRange.HasNext() {
		result, err := privateAttributeRange.Next()
		if err != nil {
			return "获取私有属性失败", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		privateAttributeMap[attribute.Key] = attribute
	}

	//根据资源查找对应的策略
	//这里暂时写死一个资源的策略

	currentTimeInt64 := time.Now().Unix() * 1000
	currentTime := strconv.FormatInt(currentTimeInt64, 10)
	if publicAttributeMap["age"].Value == "40" && privateAttributeMap["occupation"].Value == "doctor" &&
		publicAttributeMap["ip"].Value == "192.168.2.1" && currentTime >= "1669791474807" {
		var record model.Record
		record.Id = decideRequest.Id
		record.RequesterId = decideRequest.RequesterId
		record.ResourceId = decideRequest.ResourceId
		record.Response = "true"
		recordJsonAsByte, _ := json.Marshal(record)
		err := s.CreateRecord(ctx, string(recordJsonAsByte))
		if err != nil {
			return "", err
		}
		return "true", nil
	}

	return "false", nil

}

//策略决策部分 不写入访问记录 使用8个属性
func (s *SmartContract) DecideNoRecord8Attributes(ctx contractapi.TransactionContextInterface, request string) (string, error) {
	//1. 查询资源
	//2. 根据资源取回策略
	//3. 查询策略中需要的主体属性和客体属性
	//4. 根据策略进行决策

	// 先来个简易版
	var decideRequest model.DecideRequest
	err := json.Unmarshal([]byte(request), &decideRequest)
	if err != nil {
		return "false", err
	}
	//log.Printf("decideRequest:%v", decideRequest)
	//查询资源
	resourceAsByte, err := ctx.GetStub().GetState(decideRequest.ResourceId)
	if err != nil {
		return "查询资源失败", fmt.Errorf("查询资源失败")
	}
	var resource model.Resource
	err = json.Unmarshal(resourceAsByte, &resource)
	//resource, err := s.FindResourceById(ctx, decideRequest.ResourceId)
	//log.Printf("resource:%v", resource)
	//log.Printf("resource2:%v", *resource)
	//log.Printf("resource controllers:%v", resource.Controllers)
	//验证资源控制器身份
	controllerId, err := s.GetSubmittingClientIdentity(ctx)
	controllers := resource.Controllers
	i := 0
	for i < len(controllers) {
		log.Printf("controllerId: %v", controllerId)
		log.Printf("currentId: %v", controllers[i])
		if controllers[i] == controllerId {
			log.Printf("当前资源控制器身份验证通过")
			break
		}
		i++
	}
	if i == len(controllers) {
		log.Printf("i=%v\n", i)
		return "false", fmt.Errorf("资源控制器身份验证不通过")
	}

	publicAttributeMap := make(map[string]model.Attribute)
	privateAttributeMap := make(map[string]model.Attribute)
	//获取主体的公有属性
	publicAttributeStartKey := fmt.Sprintf("attribute:public:%s:", decideRequest.RequesterId)
	publicAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	publicAttributeRange, err := ctx.GetStub().GetStateByRange(publicAttributeStartKey, publicAttributeEndKey)
	if err != nil {
		return "", fmt.Errorf("获取公有属性失败")
	}
	defer publicAttributeRange.Close()
	for publicAttributeRange.HasNext() {
		result, err := publicAttributeRange.Next()
		if err != nil {
			return "", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		publicAttributeMap[attribute.Key] = attribute
	}
	//获取私有属性 attribute:private:userid:resourceid:key
	privateAttributeStartKey := fmt.Sprintf("attribute:private:%s:%s:", decideRequest.RequesterId, decideRequest.ResourceId)
	privateAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	privateAttributeRange, err := ctx.GetStub().GetStateByRange(privateAttributeStartKey, privateAttributeEndKey)
	if err != nil {
		return "", fmt.Errorf("获取私有属性失败")
	}
	defer privateAttributeRange.Close()
	for privateAttributeRange.HasNext() {
		result, err := privateAttributeRange.Next()
		if err != nil {
			return "", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		privateAttributeMap[attribute.Key] = attribute
	}
	//根据资源查找对应的策略
	//这里暂时写死一个资源的策略

	currentTimeInt64 := time.Now().Unix() * 1000
	currentTime := strconv.FormatInt(currentTimeInt64, 10)
	if publicAttributeMap["age"].Value == "40" && privateAttributeMap["occupation"].Value == "doctor" &&
		publicAttributeMap["ip"].Value == "192.168.2.1" && currentTime >= "1669791474807" &&
		publicAttributeMap["nationality"].Value == "American" && publicAttributeMap["company"].Value == "google" &&
		publicAttributeMap["group"].Value == "softDevelop" && publicAttributeMap["rank"].Value >= "8" {
		return "true", nil
	}
	return "false", nil

}

//策略决策部分 写入访问记录
func (s *SmartContract) DecideWithRecord8Attributes(ctx contractapi.TransactionContextInterface, request string) (string, error) {
	//1. 查询资源
	//2. 根据资源取回策略
	//3. 查询策略中需要的主体属性和客体属性
	//4. 根据策略进行决策

	// 先来个简易版
	var decideRequest model.DecideRequest
	err := json.Unmarshal([]byte(request), &decideRequest)
	if err != nil {
		return "请求序反序列化失败false", err
	}

	//查询资源
	//查询资源
	resourceAsByte, err := ctx.GetStub().GetState(decideRequest.ResourceId)
	if err != nil {
		return "查询资源失败", fmt.Errorf("查询资源失败")
	}
	var resource model.Resource
	err = json.Unmarshal(resourceAsByte, &resource)

	//验证资源控制器身份
	controllerId, err := s.GetSubmittingClientIdentity(ctx)
	controllers := resource.Controllers
	i := 0
	for i < len(controllers) {
		if controllers[i] == controllerId {
			break
		}
		i++
	}
	if i == len(controllers) {
		return "资源控制器身份验证不通过,false", fmt.Errorf("资源控制器身份验证不通过")
	}

	publicAttributeMap := make(map[string]model.Attribute)
	privateAttributeMap := make(map[string]model.Attribute)
	//获取主体的公有属性
	publicAttributeStartKey := fmt.Sprintf("attribute:public:%s:", decideRequest.RequesterId)
	publicAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	publicAttributeRange, err := ctx.GetStub().GetStateByRange(publicAttributeStartKey, publicAttributeEndKey)
	if err != nil {
		return "获取公有属性失败", fmt.Errorf("获取公有属性失败")
	}
	defer publicAttributeRange.Close()
	for publicAttributeRange.HasNext() {
		result, err := publicAttributeRange.Next()
		if err != nil {
			return "查询公有属性是该", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		publicAttributeMap[attribute.Key] = attribute
	}
	//获取私有属性 attribute:private:userid:resourceid:key
	privateAttributeStartKey := fmt.Sprintf("attribute:private:%s:%s:", decideRequest.RequesterId, decideRequest.ResourceId)
	privateAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	privateAttributeRange, err := ctx.GetStub().GetStateByRange(privateAttributeStartKey, privateAttributeEndKey)
	if err != nil {
		return "获取私有属性失败", fmt.Errorf("获取私有属性失败")
	}
	defer privateAttributeRange.Close()
	for privateAttributeRange.HasNext() {
		result, err := privateAttributeRange.Next()
		if err != nil {
			return "获取私有属性失败", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		privateAttributeMap[attribute.Key] = attribute
	}

	//根据资源查找对应的策略
	//这里暂时写死一个资源的策略

	currentTimeInt64 := time.Now().Unix() * 1000
	currentTime := strconv.FormatInt(currentTimeInt64, 10)

	if publicAttributeMap["age"].Value == "40" && privateAttributeMap["occupation"].Value == "doctor" &&
		publicAttributeMap["ip"].Value == "192.168.2.1" && currentTime >= "1669791474807" &&
		publicAttributeMap["nationality"].Value == "American" && publicAttributeMap["company"].Value == "google" &&
		publicAttributeMap["group"].Value == "softDevelop" && publicAttributeMap["rank"].Value >= "8" {
		var record model.Record
		record.Id = decideRequest.Id
		record.RequesterId = decideRequest.RequesterId
		record.ResourceId = decideRequest.ResourceId
		record.Response = "true"
		recordJsonAsByte, _ := json.Marshal(record)
		err := s.CreateRecord(ctx, string(recordJsonAsByte))
		if err != nil {
			return "", err
		}
		return "true", nil
	}

	return "false", nil

}

//策略决策部分 不写入访问记录 使用16个属性
func (s *SmartContract) DecideNoRecord16Attributes(ctx contractapi.TransactionContextInterface, request string) (string, error) {
	//1. 查询资源
	//2. 根据资源取回策略
	//3. 查询策略中需要的主体属性和客体属性
	//4. 根据策略进行决策

	// 先来个简易版
	var decideRequest model.DecideRequest
	err := json.Unmarshal([]byte(request), &decideRequest)
	if err != nil {
		return "false", err
	}
	//log.Printf("decideRequest:%v", decideRequest)
	//查询资源
	resourceAsByte, err := ctx.GetStub().GetState(decideRequest.ResourceId)
	if err != nil {
		return "查询资源失败", fmt.Errorf("查询资源失败")
	}
	var resource model.Resource
	err = json.Unmarshal(resourceAsByte, &resource)
	//resource, err := s.FindResourceById(ctx, decideRequest.ResourceId)
	//log.Printf("resource:%v", resource)
	//log.Printf("resource2:%v", *resource)
	//log.Printf("resource controllers:%v", resource.Controllers)
	//验证资源控制器身份
	controllerId, err := s.GetSubmittingClientIdentity(ctx)
	controllers := resource.Controllers
	i := 0
	for i < len(controllers) {
		log.Printf("controllerId: %v", controllerId)
		log.Printf("currentId: %v", controllers[i])
		if controllers[i] == controllerId {
			log.Printf("当前资源控制器身份验证通过")
			break
		}
		i++
	}
	if i == len(controllers) {
		log.Printf("i=%v\n", i)
		return "false", fmt.Errorf("资源控制器身份验证不通过")
	}

	publicAttributeMap := make(map[string]model.Attribute)
	privateAttributeMap := make(map[string]model.Attribute)
	//获取主体的公有属性
	publicAttributeStartKey := fmt.Sprintf("attribute:public:%s:", decideRequest.RequesterId)
	publicAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	publicAttributeRange, err := ctx.GetStub().GetStateByRange(publicAttributeStartKey, publicAttributeEndKey)
	if err != nil {
		return "", fmt.Errorf("获取公有属性失败")
	}
	defer publicAttributeRange.Close()
	for publicAttributeRange.HasNext() {
		result, err := publicAttributeRange.Next()
		if err != nil {
			return "", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		publicAttributeMap[attribute.Key] = attribute
	}
	//获取私有属性 attribute:private:userid:resourceid:key
	privateAttributeStartKey := fmt.Sprintf("attribute:private:%s:%s:", decideRequest.RequesterId, decideRequest.ResourceId)
	privateAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	privateAttributeRange, err := ctx.GetStub().GetStateByRange(privateAttributeStartKey, privateAttributeEndKey)
	if err != nil {
		return "", fmt.Errorf("获取私有属性失败")
	}
	defer privateAttributeRange.Close()
	for privateAttributeRange.HasNext() {
		result, err := privateAttributeRange.Next()
		if err != nil {
			return "", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		privateAttributeMap[attribute.Key] = attribute
	}
	//根据资源查找对应的策略
	//这里暂时写死一个资源的策略

	currentTimeInt64 := time.Now().Unix() * 1000
	currentTime := strconv.FormatInt(currentTimeInt64, 10)
	if publicAttributeMap["age"].Value == "40" && privateAttributeMap["occupation"].Value == "doctor" &&
		publicAttributeMap["ip"].Value == "192.168.2.1" && currentTime >= "1669791474807" &&
		publicAttributeMap["nationality"].Value == "American" && publicAttributeMap["company"].Value == "google" &&
		publicAttributeMap["group"].Value == "softDevelop" && publicAttributeMap["rank"].Value >= "8" &&
		publicAttributeMap["A1"].Value == "V1" && publicAttributeMap["A2"].Value == "V2" &&
		publicAttributeMap["A3"].Value == "V3" && publicAttributeMap["A4"].Value == "V4" &&
		publicAttributeMap["A5"].Value == "V5" && publicAttributeMap["A6"].Value == "V6" &&
		publicAttributeMap["A7"].Value == "V7" && publicAttributeMap["A8"].Value == "V8" {
		return "true", nil
	}
	return "false", nil

}

//策略决策部分 写入访问记录
func (s *SmartContract) DecideWithRecord16Attributes(ctx contractapi.TransactionContextInterface, request string) (string, error) {
	//1. 查询资源
	//2. 根据资源取回策略
	//3. 查询策略中需要的主体属性和客体属性
	//4. 根据策略进行决策

	// 先来个简易版
	var decideRequest model.DecideRequest
	err := json.Unmarshal([]byte(request), &decideRequest)
	if err != nil {
		return "请求序反序列化失败false", err
	}

	//查询资源
	//查询资源
	resourceAsByte, err := ctx.GetStub().GetState(decideRequest.ResourceId)
	if err != nil {
		return "查询资源失败", fmt.Errorf("查询资源失败")
	}
	var resource model.Resource
	err = json.Unmarshal(resourceAsByte, &resource)

	//验证资源控制器身份
	controllerId, err := s.GetSubmittingClientIdentity(ctx)
	controllers := resource.Controllers
	i := 0
	for i < len(controllers) {
		if controllers[i] == controllerId {
			break
		}
		i++
	}
	if i == len(controllers) {
		return "资源控制器身份验证不通过,false", fmt.Errorf("资源控制器身份验证不通过")
	}

	publicAttributeMap := make(map[string]model.Attribute)
	privateAttributeMap := make(map[string]model.Attribute)
	//获取主体的公有属性
	publicAttributeStartKey := fmt.Sprintf("attribute:public:%s:", decideRequest.RequesterId)
	publicAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	publicAttributeRange, err := ctx.GetStub().GetStateByRange(publicAttributeStartKey, publicAttributeEndKey)
	if err != nil {
		return "获取公有属性失败", fmt.Errorf("获取公有属性失败")
	}
	defer publicAttributeRange.Close()
	for publicAttributeRange.HasNext() {
		result, err := publicAttributeRange.Next()
		if err != nil {
			return "查询公有属性是该", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		publicAttributeMap[attribute.Key] = attribute
	}
	//获取私有属性 attribute:private:userid:resourceid:key
	privateAttributeStartKey := fmt.Sprintf("attribute:private:%s:%s:", decideRequest.RequesterId, decideRequest.ResourceId)
	privateAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	privateAttributeRange, err := ctx.GetStub().GetStateByRange(privateAttributeStartKey, privateAttributeEndKey)
	if err != nil {
		return "获取私有属性失败", fmt.Errorf("获取私有属性失败")
	}
	defer privateAttributeRange.Close()
	for privateAttributeRange.HasNext() {
		result, err := privateAttributeRange.Next()
		if err != nil {
			return "获取私有属性失败", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		privateAttributeMap[attribute.Key] = attribute
	}

	//根据资源查找对应的策略
	//这里暂时写死一个资源的策略

	currentTimeInt64 := time.Now().Unix() * 1000
	currentTime := strconv.FormatInt(currentTimeInt64, 10)

	if publicAttributeMap["age"].Value == "40" && privateAttributeMap["occupation"].Value == "doctor" &&
		publicAttributeMap["ip"].Value == "192.168.2.1" && currentTime >= "1669791474807" &&
		publicAttributeMap["nationality"].Value == "American" && publicAttributeMap["company"].Value == "google" &&
		publicAttributeMap["group"].Value == "softDevelop" && publicAttributeMap["rank"].Value >= "8" &&
		publicAttributeMap["A1"].Value == "V1" && publicAttributeMap["A2"].Value == "V2" &&
		publicAttributeMap["A3"].Value == "V3" && publicAttributeMap["A4"].Value == "V4" &&
		publicAttributeMap["A5"].Value == "V5" && publicAttributeMap["A6"].Value == "V6" &&
		publicAttributeMap["A7"].Value == "V7" && publicAttributeMap["A8"].Value == "V8" {
		var record model.Record
		record.Id = decideRequest.Id
		record.RequesterId = decideRequest.RequesterId
		record.ResourceId = decideRequest.ResourceId
		record.Response = "true"
		recordJsonAsByte, _ := json.Marshal(record)
		err := s.CreateRecord(ctx, string(recordJsonAsByte))
		if err != nil {
			return "", err
		}
		return "true", nil
	}
	return "false", nil

}

//策略决策部分 不写入访问记录 使用16个属性
func (s *SmartContract) DecideNoRecord32Attributes(ctx contractapi.TransactionContextInterface, request string) (string, error) {
	//1. 查询资源
	//2. 根据资源取回策略
	//3. 查询策略中需要的主体属性和客体属性
	//4. 根据策略进行决策

	// 先来个简易版
	var decideRequest model.DecideRequest
	err := json.Unmarshal([]byte(request), &decideRequest)
	if err != nil {
		return "false", err
	}
	//log.Printf("decideRequest:%v", decideRequest)
	//查询资源
	resourceAsByte, err := ctx.GetStub().GetState(decideRequest.ResourceId)
	if err != nil {
		return "查询资源失败", fmt.Errorf("查询资源失败")
	}
	var resource model.Resource
	err = json.Unmarshal(resourceAsByte, &resource)
	//resource, err := s.FindResourceById(ctx, decideRequest.ResourceId)
	//log.Printf("resource:%v", resource)
	//log.Printf("resource2:%v", *resource)
	//log.Printf("resource controllers:%v", resource.Controllers)
	//验证资源控制器身份
	controllerId, err := s.GetSubmittingClientIdentity(ctx)
	controllers := resource.Controllers
	i := 0
	for i < len(controllers) {
		log.Printf("controllerId: %v", controllerId)
		log.Printf("currentId: %v", controllers[i])
		if controllers[i] == controllerId {
			log.Printf("当前资源控制器身份验证通过")
			break
		}
		i++
	}
	if i == len(controllers) {
		log.Printf("i=%v\n", i)
		return "false", fmt.Errorf("资源控制器身份验证不通过")
	}

	publicAttributeMap := make(map[string]model.Attribute)
	privateAttributeMap := make(map[string]model.Attribute)
	//获取主体的公有属性
	publicAttributeStartKey := fmt.Sprintf("attribute:public:%s:", decideRequest.RequesterId)
	publicAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	publicAttributeRange, err := ctx.GetStub().GetStateByRange(publicAttributeStartKey, publicAttributeEndKey)
	if err != nil {
		return "", fmt.Errorf("获取公有属性失败")
	}
	defer publicAttributeRange.Close()
	for publicAttributeRange.HasNext() {
		result, err := publicAttributeRange.Next()
		if err != nil {
			return "", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		publicAttributeMap[attribute.Key] = attribute
	}
	//获取私有属性 attribute:private:userid:resourceid:key
	privateAttributeStartKey := fmt.Sprintf("attribute:private:%s:%s:", decideRequest.RequesterId, decideRequest.ResourceId)
	privateAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	privateAttributeRange, err := ctx.GetStub().GetStateByRange(privateAttributeStartKey, privateAttributeEndKey)
	if err != nil {
		return "", fmt.Errorf("获取私有属性失败")
	}
	defer privateAttributeRange.Close()
	for privateAttributeRange.HasNext() {
		result, err := privateAttributeRange.Next()
		if err != nil {
			return "", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		privateAttributeMap[attribute.Key] = attribute
	}
	//根据资源查找对应的策略
	//这里暂时写死一个资源的策略

	currentTimeInt64 := time.Now().Unix() * 1000
	currentTime := strconv.FormatInt(currentTimeInt64, 10)
	if publicAttributeMap["age"].Value == "40" && privateAttributeMap["occupation"].Value == "doctor" &&
		publicAttributeMap["ip"].Value == "192.168.2.1" && currentTime >= "1669791474807" &&
		publicAttributeMap["nationality"].Value == "American" && publicAttributeMap["company"].Value == "google" &&
		publicAttributeMap["group"].Value == "softDevelop" && publicAttributeMap["rank"].Value >= "8" &&
		publicAttributeMap["A1"].Value == "V1" && publicAttributeMap["A2"].Value == "V2" &&
		publicAttributeMap["A3"].Value == "V3" && publicAttributeMap["A4"].Value == "V4" &&
		publicAttributeMap["A5"].Value == "V5" && publicAttributeMap["A6"].Value == "V6" &&
		publicAttributeMap["A7"].Value == "V7" && publicAttributeMap["A8"].Value == "V8" &&
		publicAttributeMap["A9"].Value == "V9" && publicAttributeMap["A10"].Value == "V10" &&
		publicAttributeMap["A11"].Value == "V11" && publicAttributeMap["A12"].Value == "V12" &&
		publicAttributeMap["A13"].Value == "V13" && publicAttributeMap["A14"].Value == "V14" &&
		publicAttributeMap["A15"].Value == "V15" && publicAttributeMap["A16"].Value == "V16" &&
		publicAttributeMap["A17"].Value == "V17" && publicAttributeMap["A18"].Value == "V18" &&
		publicAttributeMap["A19"].Value == "V19" && publicAttributeMap["A20"].Value == "V20" &&
		publicAttributeMap["A21"].Value == "V21" && publicAttributeMap["A22"].Value == "V22" &&
		publicAttributeMap["A23"].Value == "V23" && publicAttributeMap["A24"].Value == "V24" {
		return "true", nil
	}
	return "false", nil

}

//策略决策部分 写入访问记录
func (s *SmartContract) DecideWithRecord32Attributes(ctx contractapi.TransactionContextInterface, request string) (string, error) {
	//1. 查询资源
	//2. 根据资源取回策略
	//3. 查询策略中需要的主体属性和客体属性
	//4. 根据策略进行决策

	// 先来个简易版
	var decideRequest model.DecideRequest
	err := json.Unmarshal([]byte(request), &decideRequest)
	if err != nil {
		return "请求序反序列化失败false", err
	}

	//查询资源
	//查询资源
	resourceAsByte, err := ctx.GetStub().GetState(decideRequest.ResourceId)
	if err != nil {
		return "查询资源失败", fmt.Errorf("查询资源失败")
	}
	var resource model.Resource
	err = json.Unmarshal(resourceAsByte, &resource)

	//验证资源控制器身份
	controllerId, err := s.GetSubmittingClientIdentity(ctx)
	controllers := resource.Controllers
	i := 0
	for i < len(controllers) {
		if controllers[i] == controllerId {
			break
		}
		i++
	}
	if i == len(controllers) {
		return "资源控制器身份验证不通过,false", fmt.Errorf("资源控制器身份验证不通过")
	}

	publicAttributeMap := make(map[string]model.Attribute)
	privateAttributeMap := make(map[string]model.Attribute)
	//获取主体的公有属性
	publicAttributeStartKey := fmt.Sprintf("attribute:public:%s:", decideRequest.RequesterId)
	publicAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	publicAttributeRange, err := ctx.GetStub().GetStateByRange(publicAttributeStartKey, publicAttributeEndKey)
	if err != nil {
		return "获取公有属性失败", fmt.Errorf("获取公有属性失败")
	}
	defer publicAttributeRange.Close()
	for publicAttributeRange.HasNext() {
		result, err := publicAttributeRange.Next()
		if err != nil {
			return "查询公有属性是该", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		publicAttributeMap[attribute.Key] = attribute
	}
	//获取私有属性 attribute:private:userid:resourceid:key
	privateAttributeStartKey := fmt.Sprintf("attribute:private:%s:%s:", decideRequest.RequesterId, decideRequest.ResourceId)
	privateAttributeEndKey := string(BytesPrefix([]byte(publicAttributeStartKey)))
	privateAttributeRange, err := ctx.GetStub().GetStateByRange(privateAttributeStartKey, privateAttributeEndKey)
	if err != nil {
		return "获取私有属性失败", fmt.Errorf("获取私有属性失败")
	}
	defer privateAttributeRange.Close()
	for privateAttributeRange.HasNext() {
		result, err := privateAttributeRange.Next()
		if err != nil {
			return "获取私有属性失败", fmt.Errorf("错误")
		}
		var attribute model.Attribute
		json.Unmarshal(result.Value, &attribute)
		privateAttributeMap[attribute.Key] = attribute
	}

	//根据资源查找对应的策略
	//这里暂时写死一个资源的策略

	currentTimeInt64 := time.Now().Unix() * 1000
	currentTime := strconv.FormatInt(currentTimeInt64, 10)

	if publicAttributeMap["age"].Value == "40" && privateAttributeMap["occupation"].Value == "doctor" &&
		publicAttributeMap["ip"].Value == "192.168.2.1" && currentTime >= "1669791474807" &&
		publicAttributeMap["nationality"].Value == "American" && publicAttributeMap["company"].Value == "google" &&
		publicAttributeMap["group"].Value == "softDevelop" && publicAttributeMap["rank"].Value >= "8" &&
		publicAttributeMap["A1"].Value == "V1" && publicAttributeMap["A2"].Value == "V2" &&
		publicAttributeMap["A3"].Value == "V3" && publicAttributeMap["A4"].Value == "V4" &&
		publicAttributeMap["A5"].Value == "V5" && publicAttributeMap["A6"].Value == "V6" &&
		publicAttributeMap["A7"].Value == "V7" && publicAttributeMap["A8"].Value == "V8" &&
		publicAttributeMap["A9"].Value == "V9" && publicAttributeMap["A10"].Value == "V10" &&
		publicAttributeMap["A11"].Value == "V11" && publicAttributeMap["A12"].Value == "V12" &&
		publicAttributeMap["A13"].Value == "V13" && publicAttributeMap["A14"].Value == "V14" &&
		publicAttributeMap["A15"].Value == "V15" && publicAttributeMap["A16"].Value == "V16" &&
		publicAttributeMap["A17"].Value == "V17" && publicAttributeMap["A18"].Value == "V18" &&
		publicAttributeMap["A19"].Value == "V19" && publicAttributeMap["A20"].Value == "V20" &&
		publicAttributeMap["A21"].Value == "V21" && publicAttributeMap["A22"].Value == "V23" &&
		publicAttributeMap["A24"].Value == "V24" && publicAttributeMap["A25"].Value == "V25" {
		var record model.Record
		record.Id = decideRequest.Id
		record.RequesterId = decideRequest.RequesterId
		record.ResourceId = decideRequest.ResourceId
		record.Response = "true"
		recordJsonAsByte, _ := json.Marshal(record)
		err := s.CreateRecord(ctx, string(recordJsonAsByte))
		if err != nil {
			return "", err
		}
		return "true", nil
	}
	return "false", nil

}
