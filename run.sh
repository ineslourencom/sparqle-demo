#!/bin/bash
set -Eeuo pipefail

DB_CONTAINER="order-link-db"
DB_IMAGE="postgres:latest"

DB_STARTED=0
RUN_DB=0
RUN_BACKEND=0
RUN_FRONTEND=0

declare -a CHILD_PROCESS_IDS=()
declare -a CHILD_PROCESS_GROUPS=()

usage() {
    echo "Usage: $0 [-all|-backend]"
    exit 1
}

cleanup() {
    echo "Stopping infrastructure..."


    if (( ${#CHILD_PROCESS_IDS[@]} )); then
        for process_id in "${CHILD_PROCESS_IDS[@]}"; do
            kill "${process_id}" 2>/dev/null || true
        done
    fi
    if (( ${#CHILD_PROCESS_GROUPS[@]} )); then
        for group_id in "${CHILD_PROCESS_GROUPS[@]}"; do
            kill -- "-${group_id}" 2>/dev/null || true
        done
    fi
    if (( DB_STARTED )); then
        docker stop "${DB_CONTAINER}" >/dev/null 2>&1 || true
    fi
}
trap cleanup EXIT INT TERM # stop on cleanup

start_process() {
    local name="$1"
    local cmd="$2"
    ( bash -c "${cmd}" | while IFS= read -r line || [ -n "$line" ]; do
        printf '[%s] %s\n' "${name}" "${line}"
    done ) &
    local process_id=$!
    local process_group_id=""
    process_group_id=$(ps -o pgid= "${process_id}" 2>/dev/null | tr -d ' ') || process_group_id=""
    CHILD_PROCESS_IDS+=("${process_id}")
    if [[ -n "${process_group_id}" ]]; then
        CHILD_PROCESS_GROUPS+=("${process_group_id}")
    fi
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
    start_process "compiling backend" "cd ./order-link && exec mvn clean compile"
    start_process "backend" "cd ./order-link && exec mvn spring-boot:run"
fi

if (( RUN_FRONTEND )); then
    start_process "building frontend" "cd ./order-link-frontend && exec npm install"
    start_process "frontend" "cd ./order-link-frontend && exec npm run dev"
fi

if (( RUN_DB )); then
    start_process "postgres" "docker logs -f ${DB_CONTAINER}"
fi

echo "Infrastructure setup complete. Press Ctrl+C to stop everything."
wait "${CHILD_PROCESS_IDS[@]}"