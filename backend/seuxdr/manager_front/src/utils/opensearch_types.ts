export type OpenSearchData = {
    data: Alert[];
    agent_map: AgentDetails[];
  };
  
  export type Alert = {
    _index: string;
    _id: string;
    _score: number;
    _source: Source;
    sort?: any[];
  };
  
  export type Source = {
    predecoder: Predecoder;
    agent: Agent;
    manager: Manager;
    data: Data;
    rule: Rule;
    decoder: Decoder;
    full_log: string;
    input: Input;
    "@timestamp": string;
    location: string;
    id: string;
    timestamp: string;
  };
  
  export type Predecoder = {
    hostname: string;
    program_name: string;
    timestamp: string;
  };
  
  export type Agent = {
    name: string;
    id: string;
  };
  
  export type Manager = {
    name: string;
  };
  
  export type Data = {
    srcuser: string;
    dstuser: string;
    tty: string;
    pwd: string;
    command: string;
  };
  
  export type Rule = {
    firedtimes: number;
    mail: boolean;
    level: number;
    description: string;
    groups: string[];
    mitre: Mitre;
    id: string;
  };
  
  export type Mitre = {
    technique: string[];
    id: string[];
    tactic: string[];
  };
  
  export type Decoder = {
    parent: string;
    name: string;
    ftscomment: string;
  };
  
  export type Input = {
    type: string;
  };
  
  export type AgentKey = {
    hostname: string;
    group_id: number;
  };
  
  export type AgentValue = {
    os: string;
    group_name: string;
  };
  
  export type ExtractedAlert = {
    key: string | null;
    timestamp: string | null;
    agent: string;
    group_name: string;
    os: string;
    attacker_tactic: string | string[] | null;
    description: string;
    severity: number;
  };
  
  export type AgentDetails = {
    hostname: string;
    os: string;
    group_name: string;
    group_id: number;
  };
  

