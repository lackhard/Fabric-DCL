// User describes basic details of what makes up a simple user
package model

type User struct {
	ID    string  `json:"id"`
	Money float64 `json:"money"`
	//Attributes []Attribute `json:"attributes"`
}
