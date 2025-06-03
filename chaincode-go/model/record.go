package model

type Record struct {
	Id          string `json:"id"`
	RequesterId string `json:"requesterId"`
	ResourceId  string `json:"resourceId"`
	Response    string `json:"response"`
}
