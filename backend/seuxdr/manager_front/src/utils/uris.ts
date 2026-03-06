

const ROOT_URI = import.meta.env.VITE_ROOT_URI || "https://0.0.0.0:8443";

export const VIEW_AGENTS_URI = ROOT_URI + "/api/view/agents"

export const ACTIVATE_AGENT_URI = ROOT_URI + "/api/agent/activate"

export const DEACTIVATE_AGENT_URI = ROOT_URI + "/api/agent/deactivate"

export const MANAGE_ORGS_URI= ROOT_URI + "/api/orgs"

export const MANAGE_USERS_URI= ROOT_URI + "/api/users"

export const CREATE_ORG_URI = ROOT_URI + "/api/create/org"

export const CREATE_GROUP_URI = ROOT_URI + "/api/create/group"

export const CREATE_USER_URI = ROOT_URI + "/api/create/user"

export const GET_ALERTS_URI = ROOT_URI + "/api/view/alerts"

export const GENERATE_AGENT_URI = ROOT_URI + "/api/create/agent"

export const DOWNLOAD_AGENT_URI = ROOT_URI + "/api/download/agent"

export const LOGIN_URI = ROOT_URI + "/api/login"

export const LOGOUT_URI = ROOT_URI + "/api/logout"

export const REGISTER_URI = ROOT_URI + "/api/register"

export const UPDATE_PASSWORD_URI = ROOT_URI + "/api/change-password"

export const UPDATE_USER_URI = ROOT_URI + "/api/update/user"