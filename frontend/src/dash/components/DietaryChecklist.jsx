const groups = {
  'Food allergies': ['PEANUT_FREE', 'TREE_NUT_FREE', 'SHELLFISH_FREE', 'FISH_FREE', 'EGG_FREE', 'SOY_FREE', 'SESAME_FREE'],
  'Intolerances and sensitivities': ['GLUTEN_FREE', 'DAIRY_FREE', 'LACTOSE_FREE'],
  'Nutrition preferences': ['LOW_SODIUM', 'LOW_SUGAR', 'LOW_CARB'],
  'Dietary preferences': ['VEGETARIAN', 'VEGAN', 'PESCATARIAN'],
  'Religious dietary requirements': ['HALAL', 'KOSHER'],
};

export default function DietaryChecklist({ selected, onChange, disabled }) {
  return (
    <fieldset className="dietary-checklist" disabled={disabled}>
      <legend>Dietary restrictions and preferences</legend>
      {Object.entries(groups).map(([group, options]) => (
        <div key={group}>
          <strong>{group}</strong>
          {options.map(option => (
            <label key={option}>
              <input
                type="checkbox"
                checked={selected.includes(option)}
                onChange={event => onChange(event.target.checked
                  ? [...selected, option]
                  : selected.filter(value => value !== option))}
              />
              {option.toLowerCase().replaceAll('_', ' ')}
            </label>
          ))}
        </div>
      ))}
    </fieldset>
  );
}
