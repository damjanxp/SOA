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
	fmt.Println("Tour service running on port 8083")
	http.ListenAndServe(":8083", nil)
}
