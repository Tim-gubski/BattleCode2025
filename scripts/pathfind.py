from termcolor import cprint
from time import sleep
from copy import deepcopy

TOP_LEFT =      0b1000000000000000000000000000000000000000000000000000000000000000

TOP_RIGHT =     0b0000000100000000000000000000000000000000000000000000000000000000

BOTTOM_LEFT =   0b0000000000000000000000000000000000000000000000000000000010000000

BOTTOM_RIGHT =  0b0000000000000000000000000000000000000000000000000000000000000001

TOP_EDGE =      0b1111111100000000000000000000000000000000000000000000000000000000

BOTTOM_EDGE =   0b0000000000000000000000000000000000000000000000000000000011111111

LEFT_EDGE =     0b1000000010000000100000001000000010000000100000001000000010000000

RIGHT_EDGE =    0b0000000100000001000000010000000100000001000000010000000100000001

MAP = [[1,1,1,0,1,1,1,1],
       [1,1,1,0,1,1,1,1],
       [1,1,1,0,1,1,1,1],
       [1,1,1,0,1,1,1,1],
       [1,1,1,0,1,1,1,1],
       [1,1,1,0,0,0,0,1],
       [1,1,1,1,1,1,1,1],
       [1,1,1,1,1,1,1,1]] # 8x8 2d array of boolean values

MAP2 = [[1,1,1,0,1,1,1,1,1,0],
        [1,1,1,0,1,1,1,1,1,1],
        [1,1,1,0,1,1,1,1,1,0],
        [1,1,1,0,1,1,1,1,1,1],
        [1,1,1,0,1,1,1,1,1,0],
        [1,1,1,0,0,0,0,1,1,1],
        [1,1,1,1,1,1,1,1,1,0],
        [1,1,1,1,1,1,1,1,1,1],
        [1,0,0,0,0,0,0,0,0,1],
        [1,1,1,1,1,1,1,1,1,1]] # 10x10 2d array of boolean values

# Returns an array of longs from a 2d array of boolean values, where each long represents an 8x8 local view of the 2d array
def map2longs(map):
    # pad map with 0s to make it divisible by 8 in both dimensions
    for i in range(len(map)):
        while len(map[i]) % 8 != 0:
            map[i].append(0)
    while len(map) % 8 != 0:
        map.append([0 for i in range(len(map[0]))])
    
    # printmap(map)

    longs = []
    for i in range(0, len(map), 8):
        longs.append([])
        for j in range(0, len(map[0]), 8):
            long = 0
            for k in range(8):
                for l in range(8):
                    long <<= 1
                    long |= map[i+k][j+l]
            longs[i//8].append(long)
    return longs

# Returns a 2d array of boolean values from an array of longs, where each long represents an 8x8 local view of the 2d array
def longs2map(longs):
    map = [[0 for i in range(len(longs[0])*8)] for j in range(len(longs)*8)]
    for i in range(len(longs)):
        for j in range(len(longs[0])):
            long = longs[i][j]
            for k in range(7, -1, -1):
                for l in range(7, -1, -1):
                    map[i*8+k][j*8+l] = long & 1
                    long >>= 1
    return map

def printmap(arr, passability=None, start=None, end=None):
    for i in range(len(arr)):
        for j in range(len(arr[0])):
            if not arr[i][j] and end is not None and end[0] == i and end[1] == j:
                cprint("E", "red", end="")
            elif arr[i][j]:
                if start is not None and start[0] == i and start[1] == j:
                    cprint("S", "light_magenta", end="")
                else:
                    cprint("1", "green", end="")
            else:
                if passability is not None and passability[i][j] == 0:
                    cprint("0", "blue", end="")
                else:
                    cprint("0", "yellow", end="")
        print()

# Convolve a 3x1 (vertical) kernel horizontally across a map
def convolve_horizontal(map):
    arr = deepcopy(map)
    for i in range(len(arr)):
        for j in range(len(arr[0])):
            if i == 0:
                arr[i][j] |= map[i+1][j]
            elif i == 7:
                arr[i][j] |= map[i-1][j]
            else:
                arr[i][j] |= map[i-1][j] | map[i+1][j]
    return arr


# Convolve a 1x3 (horizontal) kernel vertically across a map
def convolve_vertical(map):
    arr = deepcopy(map)
    for i in range(len(arr)):
        for j in range(len(arr[0])):
            if j == 0:
                arr[i][j] |= map[i][j+1]
            elif j == 7:
                arr[i][j] |= map[i][j-1]
            else:
                arr[i][j] |= map[i][j-1] | map[i][j+1]
    return arr

def convolve(map):
    return convolve_vertical(convolve_horizontal(map))

# Convolve a long map with a 3x3 kernel
def convolve_long(map):
    h_shift = (((map) << 1 & 0b1111111011111110111111101111111011111110111111101111111011111110) | (map >> 1 & 0b0111111101111111011111110111111101111111011111110111111101111111)) | map
    return h_shift | (h_shift >> 8) | (h_shift << 8)

def bitmask(paths, passability):
    return [[paths[i][j] & passability[i][j] for j in range(len(paths[0]))] for i in range(len(paths))]

def bitmask_long(paths, passability):
    return paths & passability

# pathfinding algorithm
def bf_pathfind(map_grid, start, end, max_steps, print_path=False):
    if map_grid[start[0]][start[1]] == 0 or map_grid[end[0]][end[1]] == 0:
        if print_path:
            print("Invalid start or end point")
        return None
    
    paths = [[0 for i in range(len(map_grid[0]))] for j in range(len(map_grid))]
    paths[start[0]][start[1]] = 1

    if print_path:
        printmap(paths, map_grid, start, end)
        sleep(1)
        print("\n---------------------------")

    for i in range(max_steps):
        paths = bitmask(convolve(paths), map_grid)
        if print_path:
            printmap(paths, map_grid, start, end)
            sleep(1)
            print("\n---------------------------")
        if paths[end[0]][end[1]] == 1:
            return paths
    
    return paths

# pathfinding algorithm using a single long (8x8) map
def bf_pathfind_long(long_map, start, end, max_steps, print_path=False):  
    if not long_map << (start[0]*8 + start[1]) & TOP_LEFT or not long_map << (end[0]*8 + end[1]) & TOP_LEFT:
        if print_path:
            print("Invalid start or end point")
        return None
    
    paths = TOP_LEFT >> (start[0]*8 + start[1])

    if print_path:
        printmap(longs2map([[paths]]), longs2map([[long_map]]), start, end)
        sleep(1)
        print("\n---------------------------")

    for i in range(max_steps):
        paths = bitmask_long(convolve_long(paths), long_map)
        if print_path:
            printmap(longs2map([[paths]]), longs2map([[long_map]]), start, end)
            sleep(1)
            print("\n---------------------------")
        if (paths << (end[0]*8 + end[1])) & TOP_LEFT:
            return paths
    
    return paths

def to_code(y_index, x_index):
    return y_index*10 + x_index

def from_code(code):
    return code // 10, code % 10

# pathfinding algorithm using a 2d array of longs
def bf_pathfind_longs(longs_map, start, end, max_steps, print_path=False):
    start_long_y = start[0] // 8
    start_long_x = start[1] // 8
    starting_long = longs_map[start_long_y][start_long_x]

    end_long_y = end[0] // 8
    end_long_x = end[1] // 8
    ending_long = longs_map[end_long_y][end_long_x]

    local_start_y = start[0] % 8
    local_start_x = start[1] % 8

    local_end_y = end[0] % 8
    local_end_x = end[1] % 8

    if not starting_long << (local_start_y*8 + local_start_x) & TOP_LEFT or not ending_long << (local_end_y*8 + local_end_x) & TOP_LEFT:
        if print_path:
            print("Invalid start or end point")
        return None
    
    paths = [[[0 for i in range(len(longs_map[0]))] for j in range(len(longs_map))] for l in range(max_steps + 1)]
    paths[0][start_long_y][start_long_x] = TOP_LEFT >> (local_start_y*8 + local_start_x)

    if print_path:
        printmap(longs2map(paths[0]), longs2map(longs_map), start, end)
        sleep(1)
        print("\n---------------------------")

    conv_list = [to_code(start_long_y, start_long_x)]

    for i in range(max_steps):
        for code in conv_list:
            y, x = from_code(code)
            paths[i+1][y][x] = convolve_long(paths[i][y][x])
            if y > 0:
                if to_code(y-1, x) in conv_list:
                    paths[i+1][y][x] |= (paths[i][y-1][x] << 56) & TOP_EDGE
                elif paths[i][y][x] & TOP_EDGE:
                    conv_list.append(to_code(y-1, x))
                    paths[i+1][y-1][x] |= (paths[i][y][x] << 56) & TOP_EDGE
            
            if y < len(longs_map) - 1:
                if to_code(y+1, x) in conv_list:
                    paths[i+1][y][x] |= (paths[i][y+1][x] >> 56) & BOTTOM_EDGE
                elif paths[i][y][x] & BOTTOM_EDGE:
                    conv_list.append(to_code(y+1, x))
                    paths[i+1][y+1][x] |= (paths[i][y][x] >> 56) & BOTTOM_EDGE

            if x > 0:
                if to_code(y, x-1) in conv_list:
                    paths[i+1][y][x] |= (paths[i][y][x-1] << 7) & LEFT_EDGE
                elif paths[i][y][x] & LEFT_EDGE:
                    conv_list.append(to_code(y, x-1))
                    paths[i+1][y][x-1] |= (paths[i][y][x] << 7) & LEFT_EDGE

            if x < len(longs_map[0]) - 1:
                if to_code(y, x+1) in conv_list:
                    paths[i+1][y][x] |= (paths[i][y][x+1] >> 7) & RIGHT_EDGE
                elif paths[i][y][x] & RIGHT_EDGE:
                    conv_list.append(to_code(y, x+1))
                    paths[i+1][y][x+1] |= (paths[i][y][x] >> 7) & RIGHT_EDGE

            paths[i+1][y][x] = bitmask_long(paths[i+1][y][x], longs_map[y][x])

        if print_path:
            printmap(longs2map(paths[i+1]), longs2map(longs_map), start, end)
            sleep(1)
            print("\n---------------------------")
        if (paths[i+1][end_long_y][end_long_x] << (local_end_y*8 + local_end_x)) & TOP_LEFT:
            return paths
    

# bf_pathfind(MAP, [4,4], [3,1], 10, True)

# bf_pathfind_long(map2longs(MAP)[0][0], [4,4], [3,1], 10, True)
        
bf_pathfind_longs(map2longs(MAP2), [4,4], [9,6], 10, True)