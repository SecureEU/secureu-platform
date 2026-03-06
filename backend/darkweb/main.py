import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from fastapi import FastAPI, Query, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from typing import List, Optional
import uvicorn
from app.darkweb_search_service import search_keyword  # Importing search function from api.py

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allow all frontend origins (use specific domain in production)
    allow_credentials=True,
    allow_methods=["*"],  # Allow all HTTP methods
    allow_headers=["*"],  # Allow all headers
)
# GET to post POST request
@app.get("/search")
def search(
    keyword: str = Query(default="bannerbuzz.com", description="Enter the keyword to search"),
    engines: Optional[List[str]] = Query(default=["clone_systems_engine"], description="List of search engines to include"),
    exclude: Optional[List[str]] = Query(default=None, description="List of search engines to exclude"),
    mp_units: int = Query(default=2, description="Number of multiprocessing units"),
    proxy: str = Query(default="localhost:9050", description="Tor proxy (default: localhost:9050)"),
    limit: int = Query(default=3, description="Set a max number of pages per engine to load"),
    continuous_write: bool = Query(default=False, description="Write progressively to output file")
):
    """ 
    Controller function that calls the `search_keyword` function from api.py 
    and returns the result.
    """
    try:
        result = search_keyword(
            keyword=keyword,
            engines=engines,
            exclude=exclude,
            mp_units=mp_units,
            proxy=proxy,
            limit=limit,
            continuous_write=continuous_write
        )
        return result  # Return the response from darkweb_search_service
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)