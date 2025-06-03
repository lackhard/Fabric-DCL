package model

type DecideRequest struct {
	Id          string `json:"id"`
	RequesterId string `json:"requesterId"`
	ResourceId  string `json:"resourceId"`
}
