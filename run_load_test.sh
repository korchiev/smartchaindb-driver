#!/bin/bash
#
# Load Test Runner for SHACL Validation Cache
#
# This script compiles and runs the LoadTestDriver with various configurations.
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored message
print_msg() {
    echo -e "${2}${1}${NC}"
}

# Print header
print_header() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════════════════╗"
    echo "║  SHACL Validation Cache - Load Test Runner                            ║"
    echo "╚════════════════════════════════════════════════════════════════════════╝"
    echo ""
}

# Check if BigchainDB is running
check_services() {
    print_msg "Checking if BigchainDB services are running..." "$BLUE"
    
    if curl -s http://localhost:9984/ > /dev/null 2>&1; then
        print_msg "✓ BigchainDB is running" "$GREEN"
    else
        print_msg "✗ BigchainDB is not accessible at http://localhost:9984/" "$RED"
        print_msg "  Run: cd ../smartchaindb && docker-compose up -d" "$YELLOW"
        exit 1
    fi
    echo ""
}

# Compile Java code
compile_code() {
    print_msg "Compiling Java code..." "$BLUE"
    
    if mvn clean compile -q; then
        print_msg "✓ Compilation successful" "$GREEN"
    else
        print_msg "✗ Compilation failed" "$RED"
        exit 1
    fi
    echo ""
}

# Run load test
run_test() {
    local mode=$1
    local count=$2
    local payload=$3
    local delay=$4
    local threads=$5
    
    print_msg "Running $mode test..." "$BLUE"
    echo ""
    
    mvn exec:java \
        -Dexec.mainClass="com.bigchaindb.smartchaindb.driver.LoadTestDriver" \
        -Dexec.args="$mode $count $payload $delay $threads" \
        -q
    
    echo ""
    print_msg "✓ Test completed" "$GREEN"
    echo ""
}

# Show usage
show_usage() {
    echo "Usage: $0 <mode> [options]"
    echo ""
    echo "Modes:"
    echo "  single       - Single transaction test"
    echo "  batch        - Batch of unique transactions"
    echo "  marketplace  - Full marketplace flow"
    echo "  stress       - Multi-threaded stress test"
    echo "  cache        - Cache effectiveness test"
    echo "  all          - Run all tests"
    echo ""
    echo "Options (mode-specific):"
    echo "  count        - Number of transactions (default: 10)"
    echo "  payloadKB    - Payload size in KB (default: 1)"
    echo "  delayMs      - Delay between TXs in ms (default: 100)"
    echo "  threads      - Number of threads for stress test (default: 1)"
    echo ""
    echo "Examples:"
    echo "  $0 single"
    echo "  $0 batch 50 2"
    echo "  $0 marketplace 5 5 1000"
    echo "  $0 stress 100 1 0 4"
    echo "  $0 cache 10"
    echo "  $0 all"
    echo ""
}

# Run all tests
run_all_tests() {
    print_msg "Running all test scenarios..." "$BLUE"
    echo ""
    
    # Test 1: Single transaction
    print_msg "═══ Test 1: Single Transaction ═══" "$YELLOW"
    run_test "single" "1" "1" "0" "1"
    sleep 2
    
    # Test 2: Small batch
    print_msg "═══ Test 2: Small Batch (10 TXs) ═══" "$YELLOW"
    run_test "batch" "10" "1" "100" "1"
    sleep 2
    
    # Test 3: Cache effectiveness
    print_msg "═══ Test 3: Cache Effectiveness ═══" "$YELLOW"
    run_test "cache" "5" "1" "100" "1"
    sleep 2
    
    # Test 4: Marketplace flow
    print_msg "═══ Test 4: Marketplace Flow (3 flows) ═══" "$YELLOW"
    run_test "marketplace" "3" "2" "1000" "1"
    sleep 2
    
    # Test 5: Larger batch with bigger payload
    print_msg "═══ Test 5: Larger Batch (20 TXs, 5KB each) ═══" "$YELLOW"
    run_test "batch" "20" "5" "200" "1"
    sleep 2
    
    print_msg "═══ All Tests Complete ═══" "$GREEN"
    
    # Fetch server metrics
    print_msg "\nFetching server-side metrics..." "$BLUE"
    if command -v jq > /dev/null 2>&1; then
        curl -s http://localhost:9984/api/v1/metrics/validation | jq '.'
    else
        curl -s http://localhost:9984/api/v1/metrics/validation
        print_msg "\nTip: Install 'jq' for prettier JSON output" "$YELLOW"
    fi
}

# Main script
main() {
    print_header
    
    if [ $# -eq 0 ]; then
        show_usage
        exit 0
    fi
    
    check_services
    compile_code
    
    mode=$1
    
    if [ "$mode" == "all" ]; then
        run_all_tests
    else
        count=${2:-10}
        payload=${3:-1}
        delay=${4:-100}
        threads=${5:-1}
        
        run_test "$mode" "$count" "$payload" "$delay" "$threads"
        
        # Show metrics command
        echo ""
        print_msg "To view server metrics, run:" "$BLUE"
        print_msg "  curl http://localhost:9984/api/v1/metrics/validation | jq" "$YELLOW"
    fi
}

main "$@"


