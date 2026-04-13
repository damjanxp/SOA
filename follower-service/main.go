package main

import (
	"fmt"
	"net/http"
)

func main() {
	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintf(w, `{"status": "OK"}`)
	})
	fmt.Println("Follower service running on port 8084")
	http.ListenAndServe(":8084", nil)
}