import { MessageCard, PageHeader } from "../components/PageState";

export default function Datatags() {
  return (
    <div>
      <PageHeader
        title="Tagged Batches"
        description="Batches annotated with an additional timestamped tag by the user."
      />
      <MessageCard
        title="No tagged batches yet"
        subtitle="Tagged data will show up here once the user has manually tagged data by pressing a button on their Pebble device."
      />
    </div>
  );
}
