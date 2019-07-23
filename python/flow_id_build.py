# 需求描述:
# 二娃提出的一个小需求，可以依次对连接进来的客户端进行分配编号
# flow_id y依赖于
# (1)olt_port_id
# (2)onu_id
# (3)tcont_id
# (4)gemport_id
# (5)pbid

# 五个属性, 每个属性发生变化对应的 flow_id 就要改变,由于 olt 的端口不知道有几个, 所以 5 个属性的每个值都是 1:n 的关系,
# flow_id 的范围为 [ 1, 16383], 要求算法能够尽可能的保证 flow_id 的唯一性, 且上下变化的步频为 5.


FLOW_ID_INFOS = [None] * 16383
ACTIVE_INDEX = 0

def build_id_info(olt_port_id, onu_id, tcont_id, gemport_id, pb_id):
    return {'olt_port_id': olt_port_id,
            'onu_id': onu_id,
            'tcont_id': tcont_id,
            'gemport_id': gemport_id,
            'pb_id': pb_id}

def assign_flow_id(id_info):
    if id_info in FLOW_ID_INFOS:
        return FLOW_ID_INFOS.index(id_info)
    else:
        global ACTIVE_INDEX
        print("get_flow_id:ACTIVE_INDEX = ", ACTIVE_INDEX)
        print("get_flow_id:id_info = ", id_info)
        FLOW_ID_INFOS[ACTIVE_INDEX] = id_info
        ACTIVE_INDEX = (ACTIVE_INDEX + 1) % 16383
        return ACTIVE_INDEX - 1

def main():
    while( "n" != input("go on or not: [y/n]: ")):
        olt_port_id = int(input("input olt_port_id: " ))
        onu_id      = int(input("input onu_ido: " )    )
        tcont_id    = int(input("input tcont_id: " )   )
        gemport_id  = int(input("input gemprot_id: " ) )
        pb_id       = int(input("input pb_id: " )      )
        id_info_dict = build_id_info(olt_port_id, onu_id, tcont_id, gemport_id, pb_id)
        print("connection info: \n", id_info_dict, "\n"
              "assign flow_id: \n", assign_flow_id(id_info_dict))

if __name__ == '__main__':
    main()

