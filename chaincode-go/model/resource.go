package model

type Resource struct {
	Id          string   `json:"id"`
	Owner       string   `json:"owner"`
	Url         string   `json:"url"`
	Description string   `json:"description"`
	Controllers []string `json:"controllers"`
}

type AddResourceControllerRequest struct {
	/**
	资源id
	*/
	ResourceId string `json:"resourceId"`
	/**
	控制器id
	*/
	ControllerId string `json:"controllerId"`
}
