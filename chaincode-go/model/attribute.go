package model

//属性id 命名规范
//attribute:public:userid:key
// attribute:private:resourceid:key
// attribute:private:userid:resourceid:key
type Attribute struct {
	Id         string  `json:"id"`
	Type       string  `json:"type"`
	ResourceId string  `json:"resourceId"`
	Owner      string  `json:"ownerId"`
	Key        string  `json:"key"`
	Value      string  `json:"value"`
	NotBefore  string  `json:"notBefore"`
	NotAfter   string  `json:"notAfter"`
	Money      float64 `json:"money"`
}
