import catalog from './exercise-catalog.json' with {type:'json'};
const normalize=value=>String(value||'').normalize('NFKC').replace(/[\u200e\u200f\u202a-\u202e\u2066-\u2069]/g,'').trim().replace(/\s+/g,' ');
const ids=new Set(catalog.map(ex=>ex.id));
const names=new Map(catalog.map(ex=>[normalize(ex.name),ex.id]));
// Older saved workouts can contain only a display name. Resolve every catalog
// name, rather than a short hand-maintained list of demo workouts.
export function resolveExercise(ex){
  const id=normalize(ex?.id);
  if(ids.has(id))return id;
  return names.get(normalize(ex?.n))||null;
}
