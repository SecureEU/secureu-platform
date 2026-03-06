import { Rule } from "antd/es/form";


export type PieData = {
  type: string;
  value: number;
};

export type ChartData = {
  text: string;
  value: string;
};

export type StackedBarData = {
  date: string
  interval: string;
  count: number;
  agent: string;
}

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

export type OpenSearchQuery = {
  query: {
    org_id: string;
    gte: string;
    lte: string;
  };
};

export type ProcessedAlert = {
	agent    : {
    name:string,
    os: string,
    group_name: string,
    id: string,
  },
	rule:   Rule,
	full_log: string,     
	timestamp: string,   
}

export type MenuItem = {
  key: string;
  icon?: React.ReactNode;
  label: React.ReactNode;
  children?: MenuItem[];
  path?: string
};


// agents received from backend in this format
export type AgentGroupView = {
  key: string;
  name: string;
  os: string;
  org_name: string;
  org_id: number;
  group_id: number;
  active: boolean;
  id: string;
  created_at: string;
  group_name: string;
};

export interface GroupJSON {
  id: number;
  created_at: string;
  name: string;
  org_id: number;
}

export interface GroupDataType {
  id: number;
  name: string;
  created_at: string;
}

export interface OrganisationJSON {
  id: number;
  created_at: string;
  name: string;
  code: string;
  groups: GroupJSON[];
}

export type Panes = {
  label: string,
  children: any,
  key: string,
}

export interface UserJSON {
  id?: string;
  first_name: string;
  last_name: string;
  email: string;
  role: string;
  password?: string;
  org_id?: number;
  group_id?: number;
}

export interface EnrichedUserJSON {
  id?: string;
  first_name: string;
  last_name: string;
  email: string;
  role: string;
  password?: string;
  org_id?: number;
  org_name?: string;
  group_id?: number;
  group_name?: string;
}