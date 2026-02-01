#!/bin/bash
set -Eeuo pipefail

DB_CONTAINER="order-link-db"
DB_IMAGE="postgres:latest"

DB_STARTED=0
RUN_DB=0
RUN_BACKEND=0
RUN_FRONTEND=0

declare -a CHILD_PIDS=()
declare -a CHILD_PGIDS=()

usage() {
    echo "Usage: $0 [-all|-backend]"
    exit 1
}

cleanup() {
    echo "Stopping infrastructure..."
    if (( ${#CHILD_PGIDS[@]} )); then
        for pgid in "${CHILD_PGIDS[@]}"; do
            kill -- "-${pgid}" 2>/dev/null || true
        done
    fi
    if (( ${#CHILD_PIDS[@]} )); then
        for pid in "${CHILD_PIDS[@]}"; do
            kill "${pid}" 2>/dev/null || true
        done
    fi
    if (( DB_STARTED )); then
        docker stop "${DB_CONTAINER}" >/dev/null 2>&1 || true
    fi
}
trap cleanup EXIT INT TERM

start_process() {
    local name="$1"
    local cmd="$2"
    ( bash -c "${cmd}" | while IFS= read -r line || [ -n "$line" ]; do
        printf '[%s] %s\n' "${name}" "${line}"
    done ) &
    local pid=$!
    local pgid
    pgid=$(ps -o pgid= "${pid}" | tr -d ' ')
    CHILD_PIDS+=("${pid}")
    CHILD_PGIDS+=("${pgid}")
}

restart_or_run_container() {
    local container_name="$1"
    local run_command="$2"

    if [ "$(docker ps -q -f name="${container_name}")" ]; then
        echo "Container '${container_name}' is running. Restarting it..."
        docker restart "${container_name}" >/dev/null
    elif [ "$(docker ps -aq -f name="${container_name}")" ]; then
        echo "Container '${container_name}' exists but is not running. Starting it..."
        docker start "${container_name}" >/dev/null
    else
        echo "Container '${container_name}' does not exist. Running it..."
        eval "${run_command}"
    fi

    DB_STARTED=1
}

if [ "$#" -eq 0 ]; then
    RUN_DB=1
    RUN_BACKEND=1
    RUN_FRONTEND=1
fi

while [[ $# -gt 0 ]]; do
    case "$1" in
        -all)
            RUN_DB=1
            RUN_BACKEND=1
            RUN_FRONTEND=1
            ;;
        -backend)
            RUN_DB=1
            RUN_BACKEND=1
            ;;
        *)
            usage
            ;;
    esac
    shift
done

if (( ! RUN_DB && ! RUN_BACKEND && ! RUN_FRONTEND )); then
    usage
fi

echo "Infrastructure setup started..."

if (( RUN_DB )); then
    echo "Starting database container..."
    restart_or_run_container "${DB_CONTAINER}" "docker run -d --name ${DB_CONTAINER} -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=admin -e POSTGRES_DB=order-link-db -p 5432:5432 ${DB_IMAGE}"
    echo "Waiting for database to start..."
    sleep 10
fi

if (( RUN_BACKEND )); then
    start_process "backend" "cd ./order-link && exec mvn spring-boot:run"
fi

if (( RUN_FRONTEND )); then
    start_process "frontend" "cd ./order-link-frontend && npm install && exec npm start"
fi

if (( RUN_DB )); then
    start_process "postgres" "docker logs -f ${DB_CONTAINER}"
fi

echo "Infrastructure setup complete. Press Ctrl+C to stop everything."
wait "${CHILD_PIDS[@]}"