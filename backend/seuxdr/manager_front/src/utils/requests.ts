import { OpenSearchQuery } from "./types";
import { GET_ALERTS_URI,MANAGE_ORGS_URI, MANAGE_USERS_URI, ACTIVATE_AGENT_URI, DEACTIVATE_AGENT_URI } from "./uris";

export const fetchWithAuth = async (url: string, options: RequestInit = {}) => {
  // No authentication required - removed Bearer token
  const headers = {
    ...(options.headers || {}),
    'Content-Type': 'application/json',
  };

  return fetch(url, {
    ...options,
    headers,
  });
};

export const fetchAlerts = async (query: OpenSearchQuery) => {
    try {
        const response = await fetchWithAuth(GET_ALERTS_URI, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify( query ),

        });
        const data = await response.json();

        return data || [];
    } catch (error) {
        console.error("Error fetching alerts:", error);
        return []; // return empty array in case of an error
    }
};



  // src/api/organisations.ts
export const fetchOrganisations = async () => {
    try {
      const response = await fetchWithAuth(MANAGE_ORGS_URI, {
        method: 'POST',
        credentials:'include',
      });
      
  
      if (!response.ok) {
        throw new Error('Failed to fetch organizations');
      }
  
      const data = await response.json();
      return data;
    } catch (err) {
      console.error("Error fetching organisations:", err);
      throw err;
    }
  };

  export const fetchUsers = async () => {
    try {
      const response = await fetchWithAuth(MANAGE_USERS_URI, {
        method: 'POST',
        credentials:'include',
      });
      
  
      if (!response.ok) {
        throw new Error('Failed to fetch users');
      }
  
      const data = await response.json();
      return data;
    } catch (err) {
      console.error("Error fetching users:", err);
      throw err;
    }
  };


// Function to activate an agent
export const activateAgent = async (agentUuid: string) => {
  try {
    const response = await fetchWithAuth(ACTIVATE_AGENT_URI, {
      method: 'POST',
      credentials: 'include',
      body: JSON.stringify({ agent_uuid: agentUuid }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data?.error || 'Failed to activate agent');
    }

    return data;
  } catch (err: any) {
    console.error('Error activating agent:', err);
    throw err;
  }
};

// Function to deactivate an agent
export const deactivateAgent = async (agentUuid: string) => {
  try {
    const response = await fetchWithAuth(DEACTIVATE_AGENT_URI, {
      method: 'POST',
      credentials: 'include',
      body: JSON.stringify({ agent_uuid: agentUuid }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data?.error || 'Failed to deactivate agent');
    }

    return data;
  } catch (err: any) {
    console.error('Error deactivating agent:', err);
    throw err;
  }
};
