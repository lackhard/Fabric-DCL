package model

type BuyAttributeRequest struct {
	Buyer       string `json:"buyer"`
	Seller      string `json:"seller"`
	AttributeId string `json:"attributeId"`
}
