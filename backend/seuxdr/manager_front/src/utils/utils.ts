

function convertToUTCAndFriendlyFormat(timestamp: string | number | Date | null): string {
    let date: Date;

    if (typeof timestamp === "string" || typeof timestamp === "number") {
      date = new Date(timestamp);
    } else if (timestamp instanceof Date) {
      date = timestamp;
    } else {
      throw new Error("Invalid timestamp format");
    }
  
    if (isNaN(date.getTime())) {
      throw new Error("Invalid date provided");
    }
  
    return date.toISOString().replace("T", " ").replace("Z", " UTC");
}

export default convertToUTCAndFriendlyFormat;


