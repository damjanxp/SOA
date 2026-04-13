FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY main.go .
RUN go mod init github.com/tim/tour-service
RUN go build -o main .

FROM alpine:latest
WORKDIR /app
COPY --from=builder /app/main .
EXPOSE 8083
CMD ["./main"]