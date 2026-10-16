export default function Icon({name,size=20,...props}:{name:string;size?:number;className?:string}) {
 const paths:Record<string,React.ReactNode> = {
 grid:<><rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/></>,
 book:<><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></>,
 calendar:<><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4m10-4v4M3 11h18m-13 4h2m4 0h2m-8 3h2"/></>,
 graph:<><rect x="9" y="2" width="6" height="5" rx="1"/><rect x="2" y="17" width="6" height="5" rx="1"/><rect x="16" y="17" width="6" height="5" rx="1"/><path d="M12 7v5M5 17v-5h14v5"/></>,
 chart:<><path d="M4 3v18h17M9 16v-5m5 5V7m5 9v-3"/></>,
 settings:<><path d="m9 3-1 3-3 1-2 4 2 2v4l4 2 3-1 3 1 4-2v-4l2-2-2-4-3-1-1-3Z"/><circle cx="12" cy="11" r="3"/></>,
 arrow:<path d="M4 12h16m-6-6 6 6-6 6"/>,
 chevron:<path d="m9 5 7 7-7 7"/>,
 check:<path d="m5 12 4 4L19 6"/>,
 plus:<path d="M12 5v14M5 12h14"/>,
 search:<><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></>,
 bell:<><path d="M5 17h14l-2-4V9a5 5 0 0 0-10 0v4Zm5 3h4"/></>,
 spark:<><path d="m12 2 2.5 7.5L22 12l-7.5 2.5L12 22l-2.5-7.5L2 12l7.5-2.5Z"/></>,
 cap:<><path d="m2 9 10-5 10 5-10 5Zm4 3v6c4 3 8 3 12 0v-6m4-3v8"/></>,
 leaf:<><path d="M20 3C8 2 2 7 5 15c8 6 16-1 15-12ZM4 21 16 8"/></>,
 clock:<><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
 info:<><circle cx="12" cy="12" r="9"/><path d="M12 11v6m0-10v1"/></>,
 download:<><path d="M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5"/></>,
 close:<path d="m6 6 12 12M6 18 18 6"/>,
 wave:<><path d="M2 9c4-6 6 6 10 0s6 6 10 0M2 15c4-6 6 6 10 0s6 6 10 0"/></>,
 logout:<><path d="M9 4H4v16h5m5-13 5 5-5 5m-6-5h11"/></>,
 shield:<><path d="m12 2 8 4v6c0 5-8 10-8 10S4 17 4 12V6Z"/><path d="m8 12 3 3 5-6"/></>,
 compass:<><circle cx="12" cy="12" r="10"/><polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/></>,
 star:<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>,
 };
 return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" strokeLinejoin="round" {...props}>{paths[name]||paths.book}</svg>
}
